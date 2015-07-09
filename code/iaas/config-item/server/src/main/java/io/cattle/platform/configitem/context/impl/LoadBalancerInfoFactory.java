package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.data.LoadBalancerTargetInfo;
import io.cattle.platform.configitem.context.data.LoadBalancerTargetsInfo;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.LoadBalancerAppCookieStickinessPolicy;
import io.cattle.platform.core.addon.LoadBalancerCookieStickinessPolicy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerInfoFactory extends AbstractAgentBaseContextFactory {

    @Inject
    LoadBalancerInstanceManager lbMgr;

    @Inject
    ObjectManager objectManager;

    @Inject
    IpAddressDao ipAddressDao;

    @Inject
    LoadBalancerDao lbDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    LoadBalancerTargetDao lbTargetDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        List<? extends LoadBalancerListener> listeners = new ArrayList<>();
        LoadBalancer lb = lbMgr.getLoadBalancerForInstance(instance);
        List<LoadBalancerTargetsInfo> targetsInfo = new ArrayList<>();
        InstanceHealthCheck lbHealthCheck = null;
        LoadBalancerAppCookieStickinessPolicy appPolicy = null;
        LoadBalancerCookieStickinessPolicy lbPolicy = null;
        if (lb != null) {
            // populate targets and listeners
            listeners = lbDao.listActiveListenersForConfig(lb.getLoadBalancerConfigId());
            if (listeners.isEmpty()) {
                return;
            }

            LoadBalancerConfig config = objectManager.loadResource(LoadBalancerConfig.class, lb.getLoadBalancerConfigId());

            appPolicy = DataAccessor.field(config, LoadBalancerConstants.FIELD_LB_APP_COOKIE_POLICY, jsonMapper, LoadBalancerAppCookieStickinessPolicy.class);

            // LEGACY: to support the case when healtcheck is defined on LB
            lbHealthCheck = DataAccessor.field(config, LoadBalancerConstants.FIELD_LB_HEALTH_CHECK, jsonMapper,
                    InstanceHealthCheck.class);

            lbPolicy = DataAccessor.field(config, LoadBalancerConstants.FIELD_LB_COOKIE_POLICY, jsonMapper, LoadBalancerCookieStickinessPolicy.class);

            targetsInfo = populateTargetsInfo(lb, lbHealthCheck);
            if (targetsInfo.isEmpty()) {
                return;
            }

        }
        context.getData().put("listeners", listeners);
        context.getData().put("publicIp", lbMgr.getLoadBalancerInstanceIp(instance).getAddress());
        context.getData().put("backends", targetsInfo);
        context.getData().put("appPolicy", appPolicy);
        context.getData().put("lbPolicy", lbPolicy);
    }


    private List<LoadBalancerTargetsInfo> populateTargetsInfo(LoadBalancer lb, InstanceHealthCheck lbHealthCheck) {
        List<? extends LoadBalancerTarget> targets = objectManager.mappedChildren(objectManager.loadResource(LoadBalancer.class, lb.getId()),
                LoadBalancerTarget.class);
        Map<Integer, List<LoadBalancerTargetInfo>> uuidToTargetInfos = new HashMap<>();
        for (LoadBalancerTarget target : targets) {
            if (!(target.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVATING) || target.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE))) {
                continue;
            }
            String ipAddress = target.getIpAddress();
            InstanceHealthCheck healthCheck = null;
            if (ipAddress == null) {
                Instance userInstance = objectManager.loadResource(Instance.class, target.getInstanceId());
                healthCheck = DataAccessor.field(userInstance,
                        InstanceConstants.FIELD_HEALTH_CHECK, jsonMapper, InstanceHealthCheck.class);

                if (userInstance.getState().equalsIgnoreCase(InstanceConstants.STATE_RUNNING)
                        || userInstance.getState().equalsIgnoreCase(InstanceConstants.STATE_STARTING)
                        || userInstance.getState().equalsIgnoreCase(InstanceConstants.STATE_RESTARTING)) {
                    for (Nic nic : objectManager.children(userInstance, Nic.class)) {
                        IpAddress ip = ipAddressDao.getPrimaryIpAddress(nic);
                        if (ip != null) {
                            ipAddress = ip.getAddress();
                            break;
                        }
                    }
                }
            }

            if (ipAddress != null) {
                String targetName = (target.getName() == null ? target.getUuid() : target.getName());
                List<LoadBalancerTargetPortSpec> portSpecs = lbTargetDao.getLoadBalancerTargetPorts(target);
                for (LoadBalancerTargetPortSpec portSpec : portSpecs) {
                    LoadBalancerTargetInfo targetInfo = new LoadBalancerTargetInfo(ipAddress, targetName,
                            target.getUuid(), portSpec, healthCheck);
                    List<LoadBalancerTargetInfo> targetInfos = uuidToTargetInfos.get(targetInfo.getUuid());
                    if (targetInfos == null) {
                        targetInfos = new ArrayList<>();
                    }

                    targetInfos.add(targetInfo);
                    uuidToTargetInfos.put(targetInfo.getUuid(), targetInfos);
                }
            }
        }

        List<LoadBalancerTargetsInfo> targetsInfo = new ArrayList<>();
        for (Integer uuid : uuidToTargetInfos.keySet()) {
            LoadBalancerTargetsInfo target = new LoadBalancerTargetsInfo(uuidToTargetInfos.get(uuid), lbHealthCheck);
            targetsInfo.add(target);
        }
        
        return targetsInfo;
    }
}
