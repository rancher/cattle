package io.cattle.platform.process.image;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.GenericObjectConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Credential;
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
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.storage.ImageCredentialLookup;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PullTaskCreate implements ProcessHandler {

    public static final String LABELS = "labels";
    public static final String IMAGE = "image";
    public static final String TAG = "tag";
    public static final String MODE = "mode";
    public static final String COMPLETE = "complete";
    public static final String STATUS = "status";
    public static final String ERRORS = "errors";

    AllocationHelper allocationHelper;
    AgentLocator agentLocator;
    ProcessProgress progress;
    ImageCredentialLookup imageCredentialLookup;
    ObjectManager objectManager;
    ObjectSerializer serializer;

    public PullTaskCreate(AllocationHelper allocationHelper, AgentLocator agentLocator, ProcessProgress progress, ImageCredentialLookup imageCredentialLookup,
                          ObjectSerializer serializer, ObjectManager objectManager) {
        super();
        this.allocationHelper = allocationHelper;
        this.agentLocator = agentLocator;
        this.progress = progress;
        this.imageCredentialLookup = imageCredentialLookup;
        this.objectManager = objectManager;
        this.serializer = serializer;
    }

    @Override
    public final HandlerResult handle(ProcessState state, ProcessInstance process) {
        if (GenericObjectConstants.KIND_PULL_TASK.equals(ObjectUtils.getKind(state.getResource()))) {
            return handleKind(state, process);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private HandlerResult handleKind(ProcessState state, ProcessInstance process) {
        GenericObject pullTask = (GenericObject)state.getResource();
        String tag = DataAccessor.fieldString(pullTask, TAG);
        String mode = DataAccessor.fieldString(pullTask, MODE);
        String image = DataAccessor.fieldString(pullTask, IMAGE);
        Credential cred = getCredential(image, pullTask.getAccountId());
        Map<String, String> labels = DataAccessor.field(pullTask, LABELS, Map.class);
        Map<String, String> status = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        if (tag == null) {
            tag = "pull-" + process.getId().hashCode();
            getData(state).withKey("tag").set(tag);
        }

        if (labels == null) {
            labels = new HashMap<>();
        }

        List<Long> hostIds = allocationHelper.getHostsSatisfyingHostAffinity(pullTask.getAccountId(), labels);
        Map<Host, ListenableFuture<? extends Event>> pullFutures = new HashMap<>();
        Map<Host, ListenableFuture<? extends Event>> cleanupFutures = new HashMap<>();

        for (final long hostId : hostIds) {
            Host host = objectManager.loadResource(Host.class, hostId);
            if (host == null) {
                return null;
            }

            ListenableFuture<? extends Event> future = pullImage(cred, host, mode, image, tag, false);
            if (future != null) {
                pullFutures.put(host, future);
            }

            if (host.getName() != null) {
                status.put(host.getName(), "Pulling");
            }
        }

        progress.init(state);
        pullTask = objectManager.reload(pullTask);
        objectManager.setFields(pullTask, STATUS, status);

        for (Map.Entry<Host, ListenableFuture<? extends Event>> entry : pullFutures.entrySet()) {
            Host host = entry.getKey();
            ListenableFuture<? extends Event> future = entry.getValue();
            progress.checkPoint("Pulling " + image + " on " + host.getName());
            try {
                AsyncUtils.get(future);
                cleanupFutures.put(host, pullImage(cred, host, mode, image, tag, true));
            } catch (EventExecutionException e) {
                pullTask = setStatus(pullTask, status, errors, host, e.getMessage(), true);
            }
        }

        for (Map.Entry<Host, ListenableFuture<? extends Event>> entry : cleanupFutures.entrySet()) {
            Host host = entry.getKey();
            ListenableFuture<? extends Event> future = entry.getValue();
            progress.checkPoint("Finishing pull " + image + " on "
                    + DataAccessor.fieldString(host, HostConstants.FIELD_HOSTNAME));
            AsyncUtils.get(future);
            pullTask = setStatus(pullTask, status, errors, host, "Done", false);
        }

        return null;
    }

    protected Credential getCredential(String uuid, long accountId) {
        return imageCredentialLookup.getDefaultCredential(uuid, accountId);
    }

    protected GenericObject setStatus(GenericObject object, Map<String, String> status, Map<String, String> errors, Host host, String message, boolean error) {
        if (host.getName() == null) {
            return object;
        }

        object = objectManager.reload(object);
        status.put(host.getName(), message);
        if (error) {
            errors.put(host.getName(), message);
        }
        return objectManager.setFields(object,
                ERRORS, errors,
                STATUS, status);
    }

    protected ListenableFuture<? extends Event> pullImage(Credential cred, Host host, String mode, String image, String tag, boolean complete) {
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
        if (cred != null) {
            CollectionUtils.setNestedValue(pullInfo, serializer.serialize(cred).get(CredentialConstants.TYPE),
                    "image", "registryCredential");
        }

        Map<String, Object> data = CollectionUtils.asMap("instancePull", (Object) pullInfo);
        Event event = new EventVO<>("compute.instance.pull").withData(data).withResourceType("resourcePull");


        return agent.call(event);
    }

    protected DataAccessor getData(ProcessState state) {
        return DataAccessor.fromMap(state.getData())
                .withScope(PullTaskCreate.class);
    }

}