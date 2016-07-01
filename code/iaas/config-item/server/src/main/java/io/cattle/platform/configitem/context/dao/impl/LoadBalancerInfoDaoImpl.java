package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.CertificateTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.configitem.context.dao.LoadBalancerInfoDao;
import io.cattle.platform.configitem.context.data.LoadBalancerListenerInfo;
import io.cattle.platform.core.addon.HaproxyConfig;
import io.cattle.platform.core.addon.LoadBalancerCookieStickinessPolicy;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.LBMetadataUtil;
import io.cattle.platform.core.util.LBMetadataUtil.LBMetadata;
import io.cattle.platform.core.util.LBMetadataUtil.StickinessPolicy;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class LoadBalancerInfoDaoImpl implements LoadBalancerInfoDao {
    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    ServiceDao svcDao;

    @Override
    @SuppressWarnings("unchecked")
    public List<LoadBalancerListenerInfo> getListeners(Service lbService) {
        Map<Integer, LoadBalancerListenerInfo> listeners = new HashMap<>();
        Map<String, Object> launchConfigData = ServiceDiscoveryUtil.getLaunchConfigDataAsMap(lbService, null);
        // 1. create listeners
        Map<String, Boolean> portDefs = new HashMap<>();

        if (launchConfigData.get(InstanceConstants.FIELD_PORTS) != null) {
            for (String port : (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS)) {
                portDefs.put(port, true);
            }
        }

        if (launchConfigData.get(InstanceConstants.FIELD_EXPOSE) != null) {
            for (String port : (List<String>) launchConfigData.get(InstanceConstants.FIELD_EXPOSE)) {
                portDefs.put(port, false);
            }
        }
        
        List<String> sslPorts = getLabeledPorts(launchConfigData, ServiceConstants.LABEL_LB_SSL_PORTS);
        List<String> proxyProtocolPorts = getLabeledPorts(launchConfigData,
                ServiceConstants.LABEL_LB_PROXY_PORTS);
        List<LoadBalancerListenerInfo> listenersToReturn = new ArrayList<>();
        for (String port : portDefs.keySet()) {
            PortSpec spec = new PortSpec(port);
            String protocol;
            if (!port.contains("tcp")) {
                // default to http unless defined otherwise in the compose file
                protocol = "http";
            } else {
                protocol = "tcp";
            }

            if (listeners.containsKey(spec.getPrivatePort())) {
                continue;
            }

            int targetPort = spec.getPrivatePort();
            Integer sourcePort = null;
            Integer privatePort = null;
            // set sourcePort only for ports defined in "ports" param
            // the ones defined in expose, will get translated to private listeners
            if (portDefs.get(port)) {
                if (spec.getPublicPort() == null) {
                    sourcePort = targetPort;
                } else {
                    sourcePort = spec.getPublicPort();
                }
                privatePort = sourcePort;
            } else {
                if (spec.getPublicPort() == null) {
                    privatePort = targetPort;
                } else {
                    privatePort = spec.getPublicPort();
                }
            }
            
            String sourceProtocol = protocol;
            if (sslPorts.contains(privatePort.toString())) {
                if (protocol.equals("tcp")) {
                    sourceProtocol = "ssl";
                } else {
                    sourceProtocol = "https";
                }
            }
            listenersToReturn.add(new LoadBalancerListenerInfo(lbService, privatePort, sourcePort,
                    sourceProtocol, targetPort, proxyProtocolPorts.contains(privatePort.toString())));
        }
        return listenersToReturn;
    }

    @SuppressWarnings("unchecked")
    protected List<String> getLabeledPorts(Map<String, Object> launchConfigData, String labelName) {
        List<String> sslPorts = new ArrayList<>();
        Map<String, String> labels = (Map<String, String>) launchConfigData.get(InstanceConstants.FIELD_LABELS);
        if (labels != null) {
            Object sslPortsObj = labels.get(labelName);
            if (sslPortsObj != null) {
                for (String sslPort : sslPortsObj.toString().split(",")) {
                    sslPorts.add(sslPort.trim());
                }
            }
        }

        return sslPorts;
    }

    @Override
    public List<LoadBalancerTargetPortSpec> getLoadBalancerTargetPorts(LoadBalancerTargetInput target,
            List<? extends LoadBalancerListenerInfo> listeners) {
        List<LoadBalancerTargetPortSpec> portSpecsInitial = new ArrayList<>();
        Map<Integer, LoadBalancerListenerInfo> lbSourcePorts = new HashMap<>();
        for (LoadBalancerListenerInfo listener : listeners) {
            lbSourcePorts.put(listener.getSourcePort(), listener);
        }

        List<Integer> targetSourcePorts = new ArrayList<>();

        List<? extends String> portsData = target.getPorts();
        if (portsData != null && !portsData.isEmpty()) {
            for (String portData : portsData) {
                portSpecsInitial.add(new LoadBalancerTargetPortSpec(portData));
            }
        }

        List<LoadBalancerTargetPortSpec> portSpecsToReturn = completePortSpecs(portSpecsInitial, listeners,
                lbSourcePorts, targetSourcePorts);

        addMissingPortSpecs(lbSourcePorts, targetSourcePorts, portSpecsToReturn);

        return portSpecsToReturn;
    }

    protected void addMissingPortSpecs(Map<Integer, LoadBalancerListenerInfo> lbSourcePorts,
            List<Integer> targetSourcePorts, List<LoadBalancerTargetPortSpec> completePortSpecs) {
        // create port specs for missing load balancer source ports
        for (Integer lbSourcePort : lbSourcePorts.keySet()) {
            if (!targetSourcePorts.contains(lbSourcePort)) {
                LoadBalancerListenerInfo listener = lbSourcePorts.get(lbSourcePort);
                completePortSpecs
                        .add(new LoadBalancerTargetPortSpec(listener.getTargetPort(), listener.getSourcePort()));
            }
        }
    }

    protected List<LoadBalancerTargetPortSpec> completePortSpecs(List<LoadBalancerTargetPortSpec> portSpecsInitial,
            List<? extends LoadBalancerListenerInfo> listeners, Map<Integer, LoadBalancerListenerInfo> lbSourcePorts,
            List<Integer> targetSourcePorts) {
        // complete missing source ports for port specs
        List<LoadBalancerTargetPortSpec> portSpecsWithSourcePorts = new ArrayList<>();
        for (LoadBalancerTargetPortSpec portSpec : portSpecsInitial) {
            if (portSpec.getSourcePort() == null) {
                for (LoadBalancerListenerInfo listener : listeners) {
                    LoadBalancerTargetPortSpec newSpec = new LoadBalancerTargetPortSpec(portSpec);
                    newSpec.setSourcePort(listener.getSourcePort());
                    portSpecsWithSourcePorts.add(newSpec);
                    // register the fact that the source port is defined on the target
                    targetSourcePorts.add(newSpec.getSourcePort());
                }
            } else {
                portSpecsWithSourcePorts.add(portSpec);
                // register the fact that the source port is defined on the target
                targetSourcePorts.add(portSpec.getSourcePort());
            }
        }

        // complete missing target ports
        List<LoadBalancerTargetPortSpec> completePortSpecs = new ArrayList<>();
        for (LoadBalancerTargetPortSpec spec : portSpecsWithSourcePorts) {
            if (spec.getPort() == null) {
                LoadBalancerListenerInfo listener = lbSourcePorts.get(spec.getSourcePort());
                if (listener != null) {
                    spec.setPort(listener.getTargetPort());
                    completePortSpecs.add(spec);
                }
            } else {
                completePortSpecs.add(spec);
            }
        }
        return completePortSpecs;
    }

    @Override
    public List<LoadBalancerTargetInput> getLoadBalancerTargetsV2(Service lbService) {
        if (!lbService.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return new ArrayList<>();
        }
        List<LoadBalancerTargetInput> targets = new ArrayList<>();
        List<? extends ServiceConsumeMap> lbLinks = consumeMapDao.findConsumedServices(lbService.getId());
        for (ServiceConsumeMap lbLink : lbLinks) {
            List<Service> consumedServices = new ArrayList<>();
            Service svc = objectManager.loadResource(Service.class, lbLink.getConsumedServiceId());
            if (sdService.isActiveService(svc)) {
                consumedServices.add(svc);
            }

            for (Service consumedService : consumedServices) {
                targets.add(new LoadBalancerTargetInput(consumedService, null, lbLink, jsonMapper));
            }
        }
        return targets;
    }

    @Override
    public List<LoadBalancerTargetInput> getLoadBalancerTargets(Service lbService) {
        if (!lbService.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return new ArrayList<>();
        }
        List<LoadBalancerTargetInput> targets = new ArrayList<>();
        List<? extends ServiceConsumeMap> lbLinks = consumeMapDao.findConsumedServices(lbService.getId());
        for (ServiceConsumeMap lbLink : lbLinks) {
            List<Service> consumedServices = new ArrayList<>();
            findConsumedServicesImpl(lbLink.getConsumedServiceId(), consumedServices);
            for (Service consumedService : consumedServices) {
                List<? extends ServiceExposeMap> exposeIpMaps = exposeMapDao.getNonRemovedServiceIpMaps(consumedService
                        .getId());
                for (ServiceExposeMap exposeIpMap : exposeIpMaps) {
                    addToTarget(targets, lbLink, exposeIpMap, consumedService);
                }
                List<? extends ServiceExposeMap> exposeInstanceMaps = exposeMapDao
                        .getNonRemovedServiceInstanceMaps(consumedService
                                .getId());
                for (ServiceExposeMap exposeInstanceMap : exposeInstanceMaps) {
                    addToTarget(targets, lbLink, exposeInstanceMap, consumedService);
                }
            }
        }

        return targets;
    }

    protected void addToTarget(List<LoadBalancerTargetInput> targets, ServiceConsumeMap lbLink,
            ServiceExposeMap exposeMap, Service service) {
        if (exposeMap.getDnsPrefix() == null) {
            targets.add(new LoadBalancerTargetInput(service, exposeMap, lbLink, jsonMapper));
        }
    }

    protected void findConsumedServicesImpl(long serviceId, List<Service> services) {
        Service service = objectManager.loadResource(Service.class, serviceId);
        if (sdService.isActiveService(service)) {
            if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)) {
                List<? extends ServiceConsumeMap> consumedMaps = consumeMapDao.findConsumedServices(serviceId);
                for (ServiceConsumeMap consumedMap : consumedMaps) {
                    if (serviceId == consumedMap.getConsumedServiceId().longValue()) {
                        continue;
                    }
                    findConsumedServicesImpl(consumedMap.getConsumedServiceId(), services);
                }
            } else {
                services.add(service);
            }
        }
    }

    protected List<Long> getLoadBalancerCertIds(Service lbService) {
        List<Long> certsToReturn = new ArrayList<>();
        for (Certificate cert : svcDao.getLoadBalancerServiceCertificates(lbService)) {
            certsToReturn.add(cert.getId());
        }
        return certsToReturn;
    }

    protected Long getLoadBalancerDefaultCertId(Service lbService) {
        Certificate defaultCert = svcDao.getLoadBalancerServiceDefaultCertificate(lbService);
        if (defaultCert != null) {
            return defaultCert.getId();
        }
        return null;
    }

    @Override
    public Map<String, Object> processLBMetadata(Service lbService, LoadBalancerInfoDao lbInfoDao,
            Map<String, Object> meta) {
        if (!lbService.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return meta;
        }

        if (meta.get(LBMetadataUtil.LB_METADATA_KEY) != null) {
            return meta;
        }

        // the logic below is to support legacy APIs by programming all the rules to metadata
        List<? extends LoadBalancerListenerInfo> listeners = lbInfoDao.getListeners(lbService);
        if (listeners.isEmpty()) {
            return meta;
        }
        // map listeners by sourcePort
        Map<Integer, LoadBalancerListenerInfo> portToListener = new HashMap<>();
        Map<Integer, List<Long>> sourcePortToServiceId = new HashMap<>();
        for (LoadBalancerListenerInfo listener : listeners) {
            portToListener.put(listener.getSourcePort(), listener);
            sourcePortToServiceId.put(listener.getSourcePort(), new ArrayList<Long>());
        }

        // get targets
        List<? extends LoadBalancerTargetInput> targets = lbInfoDao.getLoadBalancerTargetsV2(lbService);
        List<PortRule> rules = new ArrayList<>();
        List<String> registeredRules = new ArrayList<>();
        for (LoadBalancerTargetInput target : targets) {
            // get data from the port spec
            for (String portData : target.getPorts()) {
                LoadBalancerTargetPortSpec portSpec = new LoadBalancerTargetPortSpec(portData);
                List<LoadBalancerListenerInfo> listenersToRegisterTo = new ArrayList<>();
                if (portSpec.getSourcePort() != null) {
                    LoadBalancerListenerInfo l = portToListener.get(portSpec.getSourcePort());
                    // in case user specified non-existent source port
                    if (l != null) {
                        listenersToRegisterTo.add(l);
                    }
                } else {
                    listenersToRegisterTo.addAll(portToListener.values());
                }

                for (LoadBalancerListenerInfo listener : listenersToRegisterTo) {
                    List<Long> svcs = sourcePortToServiceId.get(listener.getSourcePort());
                    String path = portSpec.getPath().equalsIgnoreCase("default") ? "" : portSpec.getPath();
                    String hostname = portSpec.getDomain().equalsIgnoreCase("default") ? "" : portSpec.getDomain();
                    Integer targetPort = portSpec.getPort() != null ? portSpec.getPort() : listener.getTargetPort();
                    PortRule portRule = new PortRule(hostname, path, listener.getSourcePort(), 0,
                            PortRule.Protocol.valueOf(listener.getSourceProtocol()), String.valueOf(target.getService()
                                    .getId()),
                            targetPort, null, null);
                    if (!registeredRules.contains(getUuid(portRule))) {
                        svcs.add(target.getService().getId());
                        sourcePortToServiceId.put(listener.getSourcePort(), svcs);
                        registeredRules.add(getUuid(portRule));
                        rules.add(portRule);
                    }
                }
            }

            for (Integer sourcePort : portToListener.keySet()) {
                List<Long> svcs = sourcePortToServiceId.get(sourcePort);
                if (svcs.contains(target.getService().getId())) {
                    continue;
                }
                PortRule portRule = new PortRule("", "", sourcePort, 0, PortRule.Protocol.valueOf(portToListener.get(
                        sourcePort)
                        .getSourceProtocol()), String.valueOf(target.getService().getId()),
                        portToListener.get(sourcePort).getTargetPort(), null, null);

                if (!registeredRules.contains(getUuid(portRule))) {
                    registeredRules.add(getUuid(portRule));
                    rules.add(portRule);
                }
            }
        }

        Map<String, Object> metaToReturn = new HashMap<>();
        metaToReturn.putAll(meta);

        List<Long> certs = getLoadBalancerCertIds(lbService);
        Long defaultCert = getLoadBalancerDefaultCertId(lbService);
        Map<Long, Service> serviceIdsToService = new HashMap<>();
        Map<Long, Stack> stackIdsToStack = new HashMap<>();
        Map<Long, Certificate> certIdsToCert = new HashMap<>();
        for (Service service : objectManager.find(Service.class, SERVICE.ACCOUNT_ID,
                lbService.getAccountId(), SERVICE.REMOVED, null)) {
            serviceIdsToService.put(service.getId(), service);
        }
        
        for (Stack stack : objectManager.find(Stack.class,
                STACK.ACCOUNT_ID,
                lbService.getAccountId(), STACK.REMOVED, null)) {
            stackIdsToStack.put(stack.getId(), stack);
        }
        
        for (Certificate cert : objectManager.find(Certificate.class,
                CERTIFICATE.ACCOUNT_ID, lbService.getAccountId(), CERTIFICATE.REMOVED, null)) {
            certIdsToCert.put(cert.getId(), cert);
        }

        
        Object configObj = DataAccessor.field(lbService, ServiceConstants.FIELD_LOAD_BALANCER_CONFIG,
                Object.class);
        Map<String, Object> data = CollectionUtils.toMap(configObj);
        String config = null;
        StickinessPolicy policy = null;
        if (configObj != null) {
            LoadBalancerCookieStickinessPolicy lbPolicy = jsonMapper.convertValue(data.get(LoadBalancerConstants.FIELD_LB_COOKIE_POLICY),
                    LoadBalancerCookieStickinessPolicy.class);
            if (lbPolicy != null) {
                policy = new StickinessPolicy(lbPolicy);
            }
            HaproxyConfig customConfig = jsonMapper.convertValue(data.get(LoadBalancerConstants.FIELD_HAPROXY_CONFIG),
                    HaproxyConfig.class);
            if (customConfig != null) {
                if (!StringUtils.isEmpty(customConfig.getGlobal())) {
                    config = String.format("global\n%s\n", customConfig.getGlobal());
                }
                if (!StringUtils.isEmpty(customConfig.getDefaults())) {
                    config = String.format("%sdefaults\n%s\n", config, customConfig.getDefaults());
                }
            }
        }
        
        LBMetadata lb = new LBMetadata(rules, certs, defaultCert, serviceIdsToService, stackIdsToStack, certIdsToCert,
                config, policy);
        metaToReturn.put(LBMetadataUtil.LB_METADATA_KEY, lb);
        return metaToReturn;
    }

    private static String getUuid(PortRule rule) {
        return String.format("%s_%s_%s_%s", rule.getSourcePort(), rule.getServiceId(), rule.getHostname(), rule.getPath());
    }


}
