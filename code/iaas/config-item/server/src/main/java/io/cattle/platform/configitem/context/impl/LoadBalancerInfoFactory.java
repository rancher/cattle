
package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.LoadBalancerInfoDao;
import io.cattle.platform.configitem.context.data.LoadBalancerListenerInfo;
import io.cattle.platform.configitem.context.data.LoadBalancerTargetInfo;
import io.cattle.platform.configitem.context.data.LoadBalancerTargetsInfo;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.LoadBalancerAppCookieStickinessPolicy;
import io.cattle.platform.core.addon.LoadBalancerCookieStickinessPolicy;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.util.type.CollectionUtils;

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
    ObjectManager objectManager;

    @Inject
    IpAddressDao ipAddressDao;

    @Inject
    ServiceDao svcDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    InstanceDao instanceDao;

    @Inject
    LoadBalancerInfoDao lbInfoDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        List<? extends Service> services = instanceDao.findServicesFor(instance);
        if (services.isEmpty()) {
            return;
        }
        Service lbService = services.get(0);
        boolean sslProto = false;
        List<? extends LoadBalancerListenerInfo> listeners = lbInfoDao.getListeners(lbService);
        if (listeners.isEmpty()) {
            return;
        }
        for (LoadBalancerListenerInfo listener : listeners) {
            if (listener.getSourceProtocol().equals("https")
                    || listener.getSourceProtocol().equalsIgnoreCase("ssl")) {
                sslProto = true;
                break;
            }
        }
        
        LoadBalancerAppCookieStickinessPolicy appPolicy = null;
        LoadBalancerCookieStickinessPolicy lbPolicy = null;
        
        Object config = DataAccessor.field(lbService, ServiceDiscoveryConstants.FIELD_LOAD_BALANCER_CONFIG,
                Object.class);
        Map<String, Object> data = CollectionUtils.toMap(config);
        if (config != null) {
            appPolicy = jsonMapper.convertValue(data.get(LoadBalancerConstants.FIELD_LB_APP_COOKIE_POLICY),
                    LoadBalancerAppCookieStickinessPolicy.class);
            lbPolicy = jsonMapper.convertValue(data.get(LoadBalancerConstants.FIELD_LB_COOKIE_POLICY),
                    LoadBalancerCookieStickinessPolicy.class);
        }

        List<LoadBalancerTargetsInfo> targetsInfo = populateTargetsInfo(lbService, listeners);
        if (targetsInfo.isEmpty()) {
            return;
        }

        String haproxyConfig = DataAccessor.field(lbService, LoadBalancerConstants.FIELD_LB_HAPROXY_CONFIG,
                String.class);

        Map<String, List<LoadBalancerTargetsInfo>> listenerToTargetMap = assignTargetsToListeners(listeners,
                targetsInfo);
        context.getData().put("listeners", listeners);
        context.getData().put("publicIp", ipAddressDao.getInstancePrimaryIp(instance).getAddress());
        context.getData().put("backends", listenerToTargetMap);
        context.getData().put("appPolicy", appPolicy);
        context.getData().put("lbPolicy", lbPolicy);
        context.getData().put("sslProto", sslProto);
        context.getData().put("certs", svcDao.getLoadBalancerServiceCertificates(lbService));
        context.getData().put("defaultCert", svcDao.getLoadBalancerServiceDefaultCertificate(lbService));
        context.getData().put("haproxyConfig", haproxyConfig);

    }


    protected Map<String, List<LoadBalancerTargetsInfo>> assignTargetsToListeners(
            List<? extends LoadBalancerListenerInfo> listeners, List<LoadBalancerTargetsInfo> targetsInfo) {
        Map<String, List<LoadBalancerTargetsInfo>> listenerToTargetMap = new HashMap<>();
        for (LoadBalancerListenerInfo listener : listeners) {
            List<LoadBalancerTargetsInfo> listenerTargets = new ArrayList<>();
            for (LoadBalancerTargetsInfo info : targetsInfo) {
                Integer listnerPort = listener.getPrivatePort() == null ? listener.getSourcePort() : listener
                        .getPrivatePort();
                if (info.getPortSpec().getSourcePort().equals(listnerPort)) {
                    if (listener.getSourceProtocol().equalsIgnoreCase("http")
                            || listener.getSourceProtocol().equalsIgnoreCase("https")) {
                        listenerTargets.add(new LoadBalancerTargetsInfo(info));
                    } else {
                        // special handling for tcp ports - hostname routing rules should be ignored (by resetting the
                        // rule to Default)
                        // and all backends should be merged into one
                        LoadBalancerTargetsInfo tcpTargetsInfo = null;
                        if (listenerTargets.isEmpty()) {
                            LoadBalancerTargetPortSpec portSpec = info.getPortSpec();
                            portSpec.setDomain(LoadBalancerTargetPortSpec.DEFAULT);
                            portSpec.setPath(LoadBalancerTargetPortSpec.DEFAULT);
                            tcpTargetsInfo = new LoadBalancerTargetsInfo(info.getTargets(), info.getPortSpec());
                        } else {
                            tcpTargetsInfo = listenerTargets.get(0);
                            tcpTargetsInfo.addTargets(info.getTargets());
                        }
                        listenerTargets.clear();
                        listenerTargets.add(tcpTargetsInfo);
                    }
                }
            }
            listenerToTargetMap.put(listener.getUuid(), sortTargets(listenerTargets));
        }
        return listenerToTargetMap;
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


    private List<LoadBalancerTargetsInfo> populateTargetsInfo(Service lbService, List<? extends LoadBalancerListenerInfo> listeners) {
        List<? extends LoadBalancerTargetInput> targets = lbInfoDao.getLoadBalancerTargets(lbService);

        Map<String, List<LoadBalancerTargetInfo>> uuidToTargetInfos = new HashMap<>();
        for (LoadBalancerTargetInput target : targets) {
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
                Service service = exposeMapDao.getIpAddressService(ipAddress, lbService.getAccountId());
                if (service != null) {
                    healthCheck = DataAccessor.field(service,
                            InstanceConstants.FIELD_HEALTH_CHECK, jsonMapper, InstanceHealthCheck.class);
                }
            }

            if (ipAddress != null) {
                String targetName = target.getName();
                List<LoadBalancerTargetPortSpec> portSpecs = lbInfoDao.getLoadBalancerTargetPorts(target, listeners);
                for (LoadBalancerTargetPortSpec portSpec : portSpecs) {
                    LoadBalancerTargetInfo targetInfo = new LoadBalancerTargetInfo(ipAddress, targetName,
                            target.getName(), portSpec, healthCheck);
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
            LoadBalancerTargetsInfo target = new LoadBalancerTargetsInfo(uuidToTargetInfos.get(uuid), count);
            targetsInfo.add(target);
            count++;
        }
        
        return targetsInfo;
    }
}
