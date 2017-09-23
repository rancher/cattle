package io.cattle.platform.process.instance;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.config.DynamicIntProperty;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.exception.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class InstanceStart extends DeploymentSyncRequestHandler {

    private static final DynamicIntProperty COMPUTE_TRIES = ArchaiusUtil.getInt("instance.compute.tries");
    private static final Logger log = LoggerFactory.getLogger(InstanceStart.class);

    public InstanceStart(AgentLocator agentLocator, ObjectSerializer serializer, ObjectManager objectManager, ObjectProcessManager processManager, DeploymentSyncFactory syncFactory, MetadataManager metadataManager) {
        super(agentLocator, serializer, objectManager, processManager, syncFactory, metadataManager);
        sendNoOp = true;
        commandName = "compute.instance.activate";
        processDataKeys = Collections.singletonList(InstanceConstants.PROCESS_DATA_NO_OP);
    }

    @Override
    public HandlerResult complete(ListenableFuture<?> future, ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();

        try {
            return super.complete(future, state, process);
        } catch (ExecutionException e) {
            int tryCount = incrementComputeTry(state);
            int maxCount = getMaxComputeTries(instance);
            log.error("Failed [{}/{}] to start instance [{}]", tryCount, maxCount, instance.getId());
            if (tryCount >= maxCount) {
                return InstanceProcessManager.handleStartError(processManager, state, instance, e);
            }
            return handle(state, process);
        }
    }

    protected int incrementComputeTry(ProcessState state) {
        DataAccessor accessor = DataAccessor.fromMap(state.getData()).withScope(InstanceStart.class).withKey("computeTry");

        Integer computeTry = accessor.as(Integer.class);
        if (computeTry == null) {
            computeTry = 0;
        }

        computeTry++;

        accessor.set(computeTry);

        return computeTry;
    }

    protected int getMaxComputeTries(Instance instance) {
        Integer tries = DataAccessor.fromDataFieldOf(instance).withScope(InstanceStart.class).withKey("computeTries").as(Integer.class);

        if (tries != null && tries > 0) {
            return tries;
        }

        return COMPUTE_TRIES.get();
    }

}