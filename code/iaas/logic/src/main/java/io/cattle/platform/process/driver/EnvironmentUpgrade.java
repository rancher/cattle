package io.cattle.platform.process.driver;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.LoadBalancerInfoDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.process.common.util.ProcessUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicStringProperty;

@Named
public class EnvironmentUpgrade extends AbstractObjectProcessHandler {
    private static final DynamicStringProperty LB_IMAGE_UUID = ArchaiusUtil.getString("lb.instance.image.uuid");

    @Inject
    LoadBalancerInfoDao lbDao;
    @Inject
    ObjectManager objMgr;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    ResourceMonitor resourceMonitor;

    @Override
    public String[] getProcessNames() {
        return new String[] { "account.upgrade" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        HandlerResult result = new HandlerResult(AccountConstants.FIELD_VERSION, AccountConstants.ACCOUNT_VERSION.get());
        Account env = (Account) state.getResource();
        if (AccountConstants.PROJECT_KIND.equals(env.getKind())) {
            upgradeServices(env);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void upgradeServices(Account env) {
        List<? extends Service> lbServices = objMgr.find(Service.class, SERVICE.REMOVED, null, SERVICE.ACCOUNT_ID,
                env.getId(), SERVICE.KIND, ServiceConstants.KIND_LOAD_BALANCER_SERVICE);
        List<Service> upgradeWaitList = new ArrayList<>();
        for (Service lbService : lbServices) {
            LbConfig lbConfig = DataAccessor.field(lbService, ServiceConstants.FIELD_LB_CONFIG, jsonMapper,
                    LbConfig.class);
            Map<String, Object> existingLaunchConfig = DataAccessor.fields(lbService)
                    .withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                    .as(Map.class);
            Object image = existingLaunchConfig.get(InstanceConstants.FIELD_IMAGE_UUID);
            if (image != null && image.toString().equalsIgnoreCase(LB_IMAGE_UUID.get())) {
                upgradeWaitList.add(lbService);
                continue;
            } else {
                List<String> validUpgradeStates = Arrays.asList(CommonStatesConstants.ACTIVE,
                        CommonStatesConstants.INACTIVE, CommonStatesConstants.UPDATING_ACTIVE);
                if (validUpgradeStates.contains(lbService.getState())) {
                    // 1. set lbconfig/new launch config on the service
                    InServiceUpgradeStrategy strategy = getUpgradeStrategy(lbService);
                    lbConfig = lbDao.generateLBConfig(lbService);
                    Map<String, Object> params = new HashMap<>();
                    params.put(ServiceConstants.FIELD_LB_CONFIG, lbConfig);
                    params.put(ServiceConstants.FIELD_LAUNCH_CONFIG, strategy.getLaunchConfig());
                    lbService = objectManager.setFields(objectManager.reload(lbService), params);

                    // 2. call upgrade
                    Map<String, Object> upgradeParams = new HashMap<>();
                    upgradeParams.put(ServiceConstants.FIELD_IN_SERVICE_STRATEGY, strategy);
                    objectProcessManager.scheduleProcessInstanceAsync(ServiceConstants.PROCESS_SERVICE_UPGRADE,
                            lbService, ProcessUtils.chainInData(upgradeParams,
                                    ServiceConstants.PROCESS_SERVICE_UPGRADE,
                                    ServiceConstants.PROCESS_SERVICE_FINISH_UPGRADE));
                }
                upgradeWaitList.add(lbService);
            }
        }

        // wait for service to be upgraded
        for (Service service : upgradeWaitList) {
            Service reloaded = objectManager.reload(service);
            resourceMonitor.waitForState(reloaded, CommonStatesConstants.ACTIVE);
        }
    }

    @SuppressWarnings("unchecked")
    protected InServiceUpgradeStrategy getUpgradeStrategy(Service service) {
        Map<String, Object> existingLaunchConfig = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                .as(Map.class);
        Map<String, Object> newLaunchConfig = new HashMap<>();
        newLaunchConfig.putAll(existingLaunchConfig);
        // set ports
        if (existingLaunchConfig.containsKey(InstanceConstants.FIELD_PORTS)) {
            List<String> newPorts = new ArrayList<>();
            for (String port : (List<String>) existingLaunchConfig.get(InstanceConstants.FIELD_PORTS)) {
                PortSpec spec = new PortSpec(port);
                spec.setPrivatePort(spec.getPublicPort());
                newPorts.add(spec.toSpec());
            }
            newLaunchConfig.put(InstanceConstants.FIELD_PORTS, newPorts);
        }
        // set image
        newLaunchConfig.put(InstanceConstants.FIELD_IMAGE_UUID, LB_IMAGE_UUID.get());
        // set labels
        Map<String, String> labels = new HashMap<>();
        Object labelsObj = existingLaunchConfig.get(InstanceConstants.FIELD_LABELS);
        if (labelsObj != null) {
            labels = (Map<String, String>) labelsObj;
        }
        labels.put(SystemLabels.LABEL_AGENT_ROLE, AgentConstants.ENVIRONMENT_ADMIN_ROLE);
        labels.put(SystemLabels.LABEL_AGENT_CREATE, "true");
        newLaunchConfig.put(InstanceConstants.FIELD_LABELS, labels);
        // generate version
        String version = io.cattle.platform.util.resource.UUID.randomUUID().toString();
        newLaunchConfig.put(ServiceConstants.FIELD_VERSION, version);

        return new InServiceUpgradeStrategy(newLaunchConfig, new ArrayList<Object>(),
                existingLaunchConfig, new ArrayList<Object>(), false, 2000L, 1L);

    }
}
