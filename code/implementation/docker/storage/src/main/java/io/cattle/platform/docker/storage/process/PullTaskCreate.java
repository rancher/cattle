package io.cattle.platform.docker.storage.process;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.constants.GenericObjectConstants;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.TransitioningUtils;
import io.cattle.platform.process.common.handler.AbstractGenericObjectProcessLogic;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.util.concurrent.ListenableFuture;

public class PullTaskCreate extends AbstractGenericObjectProcessLogic implements ProcessHandler {

    public static final String LABELS = "labels";
    public static final String IMAGE = "image";
    public static final String TAG = "tag";
    public static final String MODE = "mode";
    public static final String COMPLETE = "complete";

    @Inject
    AllocatorService allocatorService;

    @Inject
    AgentLocator agentLocator;

    @Inject
    ProcessProgress progress;

    @Override
    public String[] getProcessNames() {
        return new String[] { GenericObjectConstants.PROCESS_CREATE };
    }

    @SuppressWarnings("unchecked")
    @Override
    public HandlerResult handleKind(ProcessState state, ProcessInstance process) {
        GenericObject pullTask = (GenericObject)state.getResource();
        String tag = DataAccessor.fieldString(pullTask, TAG);
        String mode = DataAccessor.fieldString(pullTask, MODE);
        String image = DataAccessor.fieldString(pullTask, IMAGE);
        Map<String, String> labels = DataAccessor.field(pullTask, LABELS, Map.class);

        if (tag == null) {
            tag = "pull-" + process.getId().hashCode();
            getData(state).withKey("tag").set(tag);
        }

        if (labels == null) {
            labels = new HashMap<>();
        }

        List<Long> hostIds = allocatorService.getHostsSatisfyingHostAffinity(pullTask.getAccountId(), labels);
        Map<Host, ListenableFuture<? extends Event>> pullFutures = new HashMap<>();
        Map<Host, ListenableFuture<? extends Event>> cleanupFutures = new HashMap<>();
        List<Integer> weights = new ArrayList<>();

        for (final long hostId : hostIds) {
            Host host = getObjectManager().loadResource(Host.class, hostId);
            if (host == null) {
                return null;
            }

            ListenableFuture<? extends Event> future = pullImage(host, mode, image, tag, false);
            if (future != null) {
                pullFutures.put(host, future);
                weights.add(1);
                weights.add(1);
            }
        }

        progress.init(state, toArray(weights));

        for (Map.Entry<Host, ListenableFuture<? extends Event>> entry : pullFutures.entrySet()) {
            Host host = entry.getKey();
            ListenableFuture<? extends Event> future = entry.getValue();
            progress.checkPoint("Pulling " + image + " on " + host.getName());
            try {
                AsyncUtils.get(future);
                cleanupFutures.put(host, pullImage(host, mode, image, tag, true));
            } catch (EventExecutionException e) {
                Map<String, Object> data = TransitioningUtils.getTransitioningData(e);
                progress.get().messsage(ObjectUtils.toString(data.get(ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD),
                        e.getTransitioningMessage()));
            }
        }

        for (Map.Entry<Host, ListenableFuture<? extends Event>> entry : cleanupFutures.entrySet()) {
            Host host = entry.getKey();
            ListenableFuture<? extends Event> future = entry.getValue();
            progress.checkPoint("Finishing pull " + image + " on " + host.getName());
            AsyncUtils.get(future);
        }

        return null;
    }

    protected ListenableFuture<? extends Event> pullImage(Host host, String mode, String image, String tag, boolean complete) {
        RemoteAgent agent = agentLocator.lookupAgent(host);
        if (agent == null) {
            return null;
        }

        DockerImage dockerImage = DockerImage.parse(image);
        if (dockerImage == null) {
            return null;
        }

        Map<String, Object> pullInfo = new HashMap<>();
        pullInfo.put(TAG, tag);
        pullInfo.put(MODE, mode);
        pullInfo.put(COMPLETE, complete);
        pullInfo.put("kind", "docker");
        CollectionUtils.setNestedValue(pullInfo, dockerImage, "image", "data", "dockerImage");

        Map<String, Object> data = CollectionUtils.asMap("instancePull", (Object) pullInfo);
        Event event = new EventVO<>("compute.instance.pull").withData(data).withResourceType("resourcePull");


        return agent.call(event);
    }

    protected int[] toArray(List<Integer> ints) {
        int[] result = new int[ints.size()];
        for (int i = 0; i < ints.size(); i++) {
            result[i] = ints.get(i);
        }
        return result;
    }

    protected DataAccessor getData(ProcessState state) {
        return DataAccessor.fromMap(state.getData())
                .withScope(PullTaskCreate.class);
    }

    @Override
    public String getKind() {
        return GenericObjectConstants.KIND_PULL_TASK;
    }

}