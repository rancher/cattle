package io.cattle.platform.configitem.context.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.configitem.context.dao.LoadBalancerInfoDao;
import io.cattle.platform.configitem.context.data.LoadBalancerListenerInfo;
import io.cattle.platform.configitem.context.data.LoadBalancerTargetInfo;
import io.cattle.platform.configitem.context.data.LoadBalancerTargetsInfo;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.addon.HaproxyConfig;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.LoadBalancerCookieStickinessPolicy;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
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
        
        LoadBalancerCookieStickinessPolicy lbPolicy = null;
        HaproxyConfig customConfig = null;
        
        Object config = DataAccessor.field(lbService, ServiceConstants.FIELD_LOAD_BALANCER_CONFIG,
                Object.class);
        Map<String, Object> data = CollectionUtils.toMap(config);
        if (config != null) {
            lbPolicy = jsonMapper.convertValue(data.get(LoadBalancerConstants.FIELD_LB_COOKIE_POLICY),
                    LoadBalancerCookieStickinessPolicy.class);
            customConfig = jsonMapper.convertValue(data.get(LoadBalancerConstants.FIELD_HAPROXY_CONFIG),
                    HaproxyConfig.class);
        }

        Object healthCheck = ServiceDiscoveryUtil.getLaunchConfigObject(lbService,
                ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME, InstanceConstants.FIELD_HEALTH_CHECK);
        InstanceHealthCheck lbHealthCheck = null;
        if (healthCheck != null) {
            lbHealthCheck = jsonMapper.convertValue(healthCheck, InstanceHealthCheck.class);
        }
        List<Instance> instances = objectManager.find(Instance.class, INSTANCE.ACCOUNT_ID, instance.getAccountId(),
                INSTANCE.REMOVED, null);
        List<Nic> nics = objectManager.find(Nic.class, NIC.ACCOUNT_ID, instance.getAccountId(),
                NIC.REMOVED, null);
        Map<Long, Instance> instanceIdtoInstance = getInstanceIdToInstance(instances);
        Map<Long, List<Nic>> instanceIdtoNic = getInstanceIdToNic(nics);
        Map<Long, IpAddress> nicIdToIp = ipAddressDao.getNicIdToPrimaryIpAddress(instance.getAccountId());
        List<LoadBalancerTargetsInfo> targetsInfo = populateTargetsInfo(lbService, listeners, instanceIdtoInstance,
                instanceIdtoNic, nicIdToIp);

        Map<String, List<LoadBalancerTargetsInfo>> listenerToTargetMap = assignTargetsToListeners(listeners,
                targetsInfo);
        context.getData().put("listeners", listeners);
        context.getData().put("publicIp", ipAddressDao.getInstancePrimaryIp(instance).getAddress());
        context.getData().put("backends", listenerToTargetMap);
        context.getData().put("lbPolicy", lbPolicy);
        context.getData().put("sslProto", sslProto);
        context.getData().put("certs", svcDao.getLoadBalancerServiceCertificates(lbService));
        context.getData().put("defaultCert", svcDao.getLoadBalancerServiceDefaultCertificate(lbService));
        context.getData().put("lbHealthCheck", lbHealthCheck);
        context.getData().put(LoadBalancerConstants.FIELD_HAPROXY_CONFIG, customConfig);
    }


    protected Map<Long, List<Nic>> getInstanceIdToNic(List<Nic> nics) {
        Map<Long, List<Nic>> instanceIdtoNic = new HashMap<>();
        for (Nic nic : nics) {
            List<Nic> instanceNics = instanceIdtoNic.get(nic.getInstanceId());
            if (instanceNics == null) {
                instanceNics = new ArrayList<>();
            }
            instanceNics.add(nic);
            instanceIdtoNic.put(nic.getInstanceId(), instanceNics);
        }
        return instanceIdtoNic;
    }


    protected Map<Long, Instance> getInstanceIdToInstance(List<Instance> instances) {
        Map<Long, Instance> instanceIdtoInstance = new HashMap<>();
        for (Instance i : instances) {
            instanceIdtoInstance.put(i.getId(), i);
        }
        return instanceIdtoInstance;
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
                return Long.compare(s2.getPortSpec().getPath().length(), s1.getPortSpec().getPath().length());
            }
        });

        List<LoadBalancerTargetsInfo> notNullDomainAndPath = new ArrayList<>();
        List<LoadBalancerTargetsInfo> notNullDomain = new ArrayList<>();
        List<LoadBalancerTargetsInfo> notNullDomainAndPathWildcard = new ArrayList<>();
        List<LoadBalancerTargetsInfo> notNullDomainWildcard = new ArrayList<>();
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
            
            String prefix = "*.";
            String suffix = ".*";
            boolean wildCard = targetInfo.getPortSpec().getDomain().startsWith(prefix)
                    || targetInfo.getPortSpec().getDomain().endsWith(suffix);

            if (pathNotNull && domainNotNull) {
                if (wildCard) {
                    notNullDomainAndPathWildcard.add(targetInfo);
                } else {
                    notNullDomainAndPath.add(targetInfo);
                }
            } else if (domainNotNull) {
                if (wildCard) {
                    notNullDomainWildcard.add(targetInfo);
                } else {
                    notNullDomain.add(targetInfo);
                }
            } else if (pathNotNull) {
                notNullPath.add(targetInfo);
            } else {
                defaultDomainAndPath.add(targetInfo);
            }
        }

        // put wildcards to the end and sort them
        toReturn.addAll(notNullDomainAndPath);
        toReturn.addAll(notNullDomain);
        toReturn.addAll(sortByDomainName(notNullDomainAndPathWildcard));
        toReturn.addAll(sortByDomainName(notNullDomainWildcard));
        toReturn.addAll(notNullPath);
        toReturn.addAll(defaultDomainAndPath);

        return toReturn;
    }

    protected List<LoadBalancerTargetsInfo> sortByDomainName(List<LoadBalancerTargetsInfo> targets) {
        Collections.sort(targets, new Comparator<LoadBalancerTargetsInfo>() {
            @Override
            public int compare(LoadBalancerTargetsInfo s1, LoadBalancerTargetsInfo s2) {
                return Long.compare(s2.getPortSpec().getPath().length(), s1.getPortSpec().getPath().length());
            }
        });
        return targets;
    }


    private List<LoadBalancerTargetsInfo> populateTargetsInfo(Service lbService,
            List<? extends LoadBalancerListenerInfo> listeners, Map<Long, Instance> instanceIdtoInstance,
            Map<Long, List<Nic>> instanceIdtoNic,
            Map<Long, IpAddress> nicIdToIp) {
        List<? extends LoadBalancerTargetInput> targets = lbInfoDao.getLoadBalancerTargets(lbService);

        Map<String, List<LoadBalancerTargetInfo>> uuidToTargetInfos = new HashMap<>();
        Map<Long, InstanceHealthCheck> svcToHC = new HashMap<>();
        for (LoadBalancerTargetInput target : targets) {
            String ipAddress = target.getIpAddress();
            InstanceHealthCheck healthCheck = null;
            if (ipAddress == null) {
                Instance targetInstance = instanceIdtoInstance.get(target.getInstanceId());
                // instance can be removed at this point
                if (targetInstance == null) {
                    continue;
                }

                healthCheck = svcToHC.get(target.getService().getId());
                if (healthCheck == null) {
                    Object hC = ServiceDiscoveryUtil.getLaunchConfigObject(target.getService(),
                            ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME, InstanceConstants.FIELD_HEALTH_CHECK);
                    if (hC != null) {
                        healthCheck = jsonMapper.convertValue(hC, InstanceHealthCheck.class);
                        svcToHC.put(target.getService().getId(), healthCheck);
                    }
                }

                if (targetInstance.getState().equalsIgnoreCase(InstanceConstants.STATE_RUNNING)
                        || targetInstance.getState().equalsIgnoreCase(InstanceConstants.STATE_STARTING)
                        || targetInstance.getState().equalsIgnoreCase(InstanceConstants.STATE_RESTARTING)) {
                    List<Nic> nics = instanceIdtoNic.get(targetInstance.getId());
                    if (nics != null) {
                        for (Nic nic : nics) {
                            IpAddress ip = nicIdToIp.get(nic.getId());
                            if (ip != null) {
                                ipAddress = ip.getAddress();
                                break;
                            }
                        }
                    }
                }
            } else {
                Service service = target.getService();
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
