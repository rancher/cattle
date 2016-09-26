package io.cattle.platform.configitem.context.dao.impl;

import io.cattle.platform.configitem.context.dao.LoadBalancerInfoDao;
import io.cattle.platform.configitem.context.data.LoadBalancerListenerInfo;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
            lbSourcePorts.put(getSourcePort(listener), listener);
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
                        .add(new LoadBalancerTargetPortSpec(listener.getTargetPort(), getSourcePort(listener)));
            }
        }
    }

    protected Integer getSourcePort(LoadBalancerListenerInfo listener) {
        // LEGACY code to support the case when private port is not defined
        return listener.getPrivatePort() != null ? listener.getPrivatePort() : listener.getSourcePort();
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
                    newSpec.setSourcePort(getSourcePort(listener));
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
}
