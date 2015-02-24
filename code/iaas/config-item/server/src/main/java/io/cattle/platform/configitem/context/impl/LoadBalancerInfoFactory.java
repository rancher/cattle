package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.data.LoadBalancerTargetInfo;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.addon.LoadBalancerAppCookieStickinessPolicy;
import io.cattle.platform.core.addon.LoadBalancerCookieStickinessPolicy;
import io.cattle.platform.core.addon.LoadBalancerHealthCheck;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        List<? extends LoadBalancerListener> listeners = new ArrayList<>();
        LoadBalancer lb = lbMgr.getLoadBalancerForInstance(instance);
        List<LoadBalancerTargetInfo> targetsInfo = new ArrayList<>();
        LoadBalancerHealthCheck healthCheck = null;
        LoadBalancerAppCookieStickinessPolicy appPolicy = null;
        LoadBalancerCookieStickinessPolicy lbPolicy = null;
        if (lb != null) {
            // populate targets and listeners
            listeners = lbDao.listActiveListenersForConfig(lb.getLoadBalancerConfigId());
            if (listeners.isEmpty()) {
                return;
            }
            targetsInfo = populateTargetsInfo(lb);
            if (targetsInfo.isEmpty()) {
                return;
            }

            LoadBalancerConfig config = objectManager.loadResource(LoadBalancerConfig.class,
                    lb.getLoadBalancerConfigId());
            healthCheck = DataAccessor.field(config, LoadBalancerConstants.FIELD_LB_HEALTH_CHECK,
                    jsonMapper,
                    LoadBalancerHealthCheck.class);

            appPolicy = DataAccessor.field(config, LoadBalancerConstants.FIELD_LB_APP_COOKIE_POLICY, jsonMapper,
                    LoadBalancerAppCookieStickinessPolicy.class);

            lbPolicy = DataAccessor.field(config, LoadBalancerConstants.FIELD_LB_COOKIE_POLICY, jsonMapper,
                    LoadBalancerCookieStickinessPolicy.class);
        }
        context.getData().put("listeners", listeners);
        context.getData().put("publicIp", lbMgr.getLoadBalancerInstanceIp(instance).getAddress());
        context.getData().put("targets", targetsInfo);
        context.getData().put("healthCheck", healthCheck);
        context.getData().put("appPolicy", appPolicy);
        context.getData().put("lbPolicy", lbPolicy);
    }

    private List<LoadBalancerTargetInfo> populateTargetsInfo(LoadBalancer lb) {
        List<? extends LoadBalancerTarget> targets = objectManager.mappedChildren(
                objectManager.loadResource(LoadBalancer.class, lb.getId()),
                LoadBalancerTarget.class);
        List<LoadBalancerTargetInfo> targetsInfo = new ArrayList<>();
        for (LoadBalancerTarget target : targets) {
            if (!(target.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVATING) || target.getState()
                    .equalsIgnoreCase(CommonStatesConstants.ACTIVE))) {
                continue;
            }
            String ipAddress = target.getIpAddress();
            if (ipAddress == null) {
                Instance userInstance = objectManager.loadResource(Instance.class, target.getInstanceId());
                if (userInstance.getState().equalsIgnoreCase(InstanceConstants.STATE_RUNNING)
                        || userInstance.getState().equalsIgnoreCase(InstanceConstants.STATE_STARTING)) {
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
                targetsInfo.add(new LoadBalancerTargetInfo(ipAddress, targetName, target.getUuid()));
            }
        }
        return targetsInfo;
    }
}
