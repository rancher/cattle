package io.cattle.platform.inator.process;

import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.ServiceUpgrade;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.inator.InatorLifecycleManager;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InatorReconcileHandler extends AbstractObjectProcessHandler implements Priority {

    @Inject
    InatorLifecycleManager lifecycleManager;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] {"service.*", "deploymentunit.*"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        String resourceState = ObjectUtils.getState(state.getResource());
        if (CommonStatesConstants.REGISTERING.equals(resourceState) || CommonStatesConstants.ERRORING.equals(resourceState)) {
            return null;
        }

        if (state.getResource() instanceof Service) {
            setBatchFields(state, process);
        }

        lifecycleManager.handleProcess(process.getName(), state.getResource(), Long.parseLong(state.getResourceId()));
        objectManager.reload(state.getResource());
        return null;
    }

    protected void setBatchFields(ProcessState state, ProcessInstance process) {
         ServiceUpgrade upgrade = jsonMapper.convertValue(state.getData(), ServiceUpgrade.class);
         Service service = (Service)state.getResource();

         Map<String, Object> updates = new HashMap<>();
         if (upgrade == null) {
             return;
         }

         InServiceUpgradeStrategy strategy = upgrade.getInServiceStrategy();
         if (strategy == null) {
             return;
         }

         updates.put(ServiceConstants.FIELD_START_FIRST_ON_UPGRADE, strategy.getStartFirst());
         if (strategy.getBatchSize() != null) {
             updates.put(ServiceConstants.FIELD_BATCHSIZE, strategy.getBatchSize());
         }
         if (strategy.getIntervalMillis() != null) {
             updates.put(ServiceConstants.FIELD_INTERVAL_MILLISEC, strategy.getIntervalMillis());
         }
         objectManager.setFields(service, updates);
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

}