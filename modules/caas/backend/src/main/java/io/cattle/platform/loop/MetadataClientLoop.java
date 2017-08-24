package io.cattle.platform.loop;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.addon.MetadataSyncRequest;
import io.cattle.platform.core.addon.metadata.EnvironmentInfo;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.eventing.exception.AgentRemovedException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.util.resource.UUID;
import io.cattle.platform.util.type.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetadataClientLoop implements Loop {

    private static final Logger log = LoggerFactory.getLogger(MetadataClientLoop.class);

    long agentId;
    String generation;
    MetadataSyncRequest toSend;
    ScheduledExecutorService scheduledExecutorService;
    ObjectManager objectManager;
    MetadataManager metadataManager;
    AgentLocator agentLocator;
    boolean inFlight;
    ObjectSerializer objectSerializer;

    public MetadataClientLoop(long agentId,
                              AgentLocator agentLocator,
                              MetadataManager metadataManager,
                              ObjectManager objectManager,
                              ObjectSerializer objectSerializer,
                              ScheduledExecutorService scheduledExecutorService) {
        this.agentId = agentId;
        this.agentLocator = agentLocator;
        this.scheduledExecutorService = scheduledExecutorService;
        this.objectManager = objectManager;
        this.objectSerializer = objectSerializer;
        this.metadataManager = metadataManager;

        reset();
    }

    private synchronized void reset() {
        Agent agent = objectManager.loadResource(Agent.class, agentId);
        Metadata metadata = metadataManager.getMetadataForAccount(agent.getResourceAccountId());

        generation = UUID.randomUUID().toString();
        toSend = new MetadataSyncRequest();
        toSend.setFull(true);
        toSend.putAllUpdates(metadata.getAll());

        for (EnvironmentInfo env : metadata.getEnvironments()) {
            if (!env.isSystem()) {
                Metadata nonSystem = metadataManager.getMetadataForAccount(env.getAccountId());
                toSend.putAllUpdates(nonSystem.getAll());
            }
        }
    }

    @Override
    public synchronized Result run(List<Object> input) {
        if (input.size() == 0 && !inFlight) {
            return Result.DONE;
        }

        for (Object obj : input) {
            toSend.add(obj);
        }

        if (!inFlight) {
            send();
        }

        return Result.WAITING;
    }

    private synchronized void failed(MetadataSyncRequest inFlightRequest, Throwable t) {
        if (t instanceof TimeoutException) {
            log.info("Failed to sync metadata with agent [{}] [{}]", agentId, t.getMessage());
        } else if (t instanceof AgentRemovedException) {
            // Zero out the queues so the loop goes to sleep
            inFlightRequest = new MetadataSyncRequest();
            toSend = new MetadataSyncRequest();
        } else {
            log.error("Failed to sync metadata with agent [{}] [{}]", agentId, t);
        }
        inFlightRequest.putAll(toSend);
        toSend = inFlightRequest;
        scheduledExecutorService.schedule(MetadataClientLoop.this::send, 2, TimeUnit.SECONDS);
    }

    private synchronized void response(Event event) {
        Map<String, Object> data = CollectionUtils.toMap(event.getData());
        Object reload = data.get("reload");
        if (reload instanceof Boolean && (Boolean)reload) {
            reset();
        }
        send();
    }

    private synchronized void send() {
        MetadataSyncRequest inFlightRequest = toSend;
        toSend = new MetadataSyncRequest();
        inFlight = true;

        if (inFlightRequest.size() == 0) {
            inFlight = false;
            return;
        }

        inFlightRequest.setGeneration(generation);

        RemoteAgent agent = agentLocator.lookupAgent(agentId);
        Event event = EventVO.newEvent(FrameworkEvents.METADATA_SYNC)
                .withData(objectSerializer.serialize(inFlightRequest));

        // I swear I hate futures.
        Futures.addCallback(agent.call(event), new FutureCallback<Event>() {
            @Override
            public void onSuccess(@Nullable Event result) {
                response(result);
            }

            @Override
            public void onFailure(Throwable t) {
                failed(inFlightRequest, t);
            }
        });
    }

}
