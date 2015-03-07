package io.cattle.platform.agent.connection.delegate;

import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.server.connection.AgentConnection;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventProgress;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.eventing.util.EventUtils;
import io.cattle.platform.iaas.event.delegate.DelegateEvent;
import io.cattle.platform.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class AgentDelegateConnection implements AgentConnection {

    private static final Logger log = LoggerFactory.getLogger(AgentDelegateConnection.class);

    String uri;
    long agentId;
    boolean open = true;
    RemoteAgent remoteAgent;
    Map<String, Object> instanceData;
    JsonMapper jsonMapper;
    EventService eventService;

    public AgentDelegateConnection(RemoteAgent remoteAgent, long agentId, String uri, Map<String, Object> instanceData, JsonMapper jsonMapper,
            EventService eventService) {
        super();
        this.remoteAgent = remoteAgent;
        this.agentId = agentId;
        this.uri = uri;
        this.jsonMapper = jsonMapper;
        this.instanceData = instanceData;
        this.eventService = eventService;
    }

    @Override
    public long getAgentId() {
        return agentId;
    }

    @Override
    public ListenableFuture<Event> execute(final Event event, final EventProgress progress) {
        if (!open) {
            return AsyncUtils.error(new IOException("Agent connection is closed"));
        }

        final DelegateEvent delegateEvent = new DelegateEvent(instanceData, event);

        log.trace("Delegating [{}] [{}] inner [{}] [{}]", event.getName(), event.getId(), delegateEvent.getName(), delegateEvent.getId());

        EventCallOptions options = EventUtils.chainOptions(event).withProgress(new EventProgress() {
            @Override
            public void progress(Event delegateResponse) {
                Event reply = createResponse(delegateEvent, delegateResponse);
                if (EventUtils.isTransitioningEvent(reply)) {
                    progress.progress(reply);
                }
            }
        });

        return Futures.transform(remoteAgent.call(delegateEvent, options), new AsyncFunction<Event, Event>() {
            @Override
            public ListenableFuture<Event> apply(final Event delegateResponse) throws Exception {
                return AsyncUtils.done((Event) createResponse(delegateEvent, delegateResponse));
            }
        });
    }

    protected EventVO<Object> createResponse(Event delegateRequest, Event delegateResponse) {
        if (delegateResponse == null) {
            return null;
        }

        Object data = delegateResponse.getData();

        if (data == null) {
            return null;
        }

        EventVO<?> insideEvent = jsonMapper.convertValue(data, EventVO.class);

        EventVO<Object> response = EventVO.reply(delegateRequest);
        response.setData(insideEvent.getData());

        EventUtils.copyTransitioning(insideEvent, response);

        return response;
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

}