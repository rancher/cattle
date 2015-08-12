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
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lb.instance.service.LoadBalancerInstanceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    @Inject
    ServiceExposeMapDao exposeMapDao;

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
        context.getData().put("backends", sortTargets(targetsInfo));
        context.getData().put("appPolicy", appPolicy);
        context.getData().put("lbPolicy", lbPolicy);

        context.getData().put("certs", lbDao.getLoadBalancerCertificates(lb));
        context.getData().put("defaultCert", lbDao.getLoadBalancerDefaultCertificate(lb));
    }


    protected List<LoadBalancerTargetsInfo> sortTargets(List<LoadBalancerTargetsInfo> targetsInfo) {
        List<LoadBalancerTargetsInfo> toReturn = new ArrayList<>();
        // sort by path length first
        Collections.sort(targetsInfo, new Comparator<LoadBalancerTargetsInfo>() {
            @Override
            public int compare(LoadBalancerTargetsInfo s1, LoadBalancerTargetsInfo s2) {
                return s1.getPortSpec().getPath().length() >= s2.getPortSpec().getPath().length() ? -1 : 1;
            }
        });
        List<LoadBalancerTargetsInfo> notNullDomainAndPath = new ArrayList<>();
        List<LoadBalancerTargetsInfo> notNullDomain = new ArrayList<>();
        List<LoadBalancerTargetsInfo> notNullPath = new ArrayList<>();
        List<LoadBalancerTargetsInfo> defaultDomainAndPath = new ArrayList<>();
        /*
         * The order on haproxy should be as follows (from top to bottom):
         * 1) acls with domain/url
         * 2) acls with domain
         * 3) acls with /url
         * 4) default rules
         */
        for (LoadBalancerTargetsInfo targetInfo : targetsInfo) {
            boolean pathNotNull = !targetInfo.getPortSpec().getPath()
                    .equalsIgnoreCase(LoadBalancerTargetPortSpec.DEFAULT);
            boolean domainNotNull = !targetInfo.getPortSpec().getDomain()
                    .equalsIgnoreCase(LoadBalancerTargetPortSpec.DEFAULT);
            
            if (pathNotNull && domainNotNull) {
                notNullDomainAndPath.add(targetInfo);
            } else if (domainNotNull) {
                notNullDomain.add(targetInfo);
            } else if (pathNotNull) {
                notNullPath.add(targetInfo);
            } else {
                defaultDomainAndPath.add(targetInfo);
            }
        }
        toReturn.addAll(notNullDomainAndPath);
        toReturn.addAll(notNullDomain);
        toReturn.addAll(notNullPath);
        toReturn.addAll(defaultDomainAndPath);

        return toReturn;
    }


    private List<LoadBalancerTargetsInfo> populateTargetsInfo(LoadBalancer lb, InstanceHealthCheck lbHealthCheck) {
        List<? extends LoadBalancerTarget> targets = objectManager.mappedChildren(objectManager.loadResource(LoadBalancer.class, lb.getId()),
                LoadBalancerTarget.class);
        Map<String, List<LoadBalancerTargetInfo>> uuidToTargetInfos = new HashMap<>();
        for (LoadBalancerTarget target : targets) {
            if (!(target.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVATING)
                    || target.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE) || target.getState()
                    .equalsIgnoreCase(CommonStatesConstants.UPDATING_ACTIVE))) {
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
            } else {
                Service service = exposeMapDao.getIpAddressService(ipAddress, target.getAccountId());
                if (service != null) {
                    healthCheck = DataAccessor.field(service,
                            InstanceConstants.FIELD_HEALTH_CHECK, jsonMapper, InstanceHealthCheck.class);
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
        int count = 0;
        for (String uuid : uuidToTargetInfos.keySet()) {
            LoadBalancerTargetsInfo target = new LoadBalancerTargetsInfo(uuidToTargetInfos.get(uuid), lbHealthCheck,
                    count);
            targetsInfo.add(target);
            count++;
        }
        
        return targetsInfo;
    }
}
