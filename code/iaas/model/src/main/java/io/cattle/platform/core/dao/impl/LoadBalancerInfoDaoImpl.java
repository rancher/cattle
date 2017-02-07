package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.CertificateTable.*;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.core.addon.HaproxyConfig;
import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.addon.LoadBalancerCookieStickinessPolicy;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.LoadBalancerInfoDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.LBMetadataUtil.LBConfigMetadataStyle;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class LoadBalancerInfoDaoImpl implements LoadBalancerInfoDao {
    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceDao svcDao;

    @SuppressWarnings("unchecked")
    protected List<LoadBalancerListenerInfo> getListeners(Service lbService) {
        Map<Integer, LoadBalancerListenerInfo> listeners = new HashMap<>();
        Map<String, Object> launchConfig = DataAccessor.fields(lbService)
                .withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                .as(Map.class);
        // 1. create listeners
        Map<String, Boolean> portDefs = new HashMap<>();

        if (launchConfig.get(InstanceConstants.FIELD_PORTS) != null) {
            for (String port : (List<String>) launchConfig.get(InstanceConstants.FIELD_PORTS)) {
                portDefs.put(port, true);
            }
        }

        if (launchConfig.get(InstanceConstants.FIELD_EXPOSE) != null) {
            for (String port : (List<String>) launchConfig.get(InstanceConstants.FIELD_EXPOSE)) {
                portDefs.put(port, false);
            }
        }

        List<String> sslPorts = getLabeledPorts(launchConfig, ServiceConstants.LABEL_LB_SSL_PORTS);
        List<String> proxyProtocolPorts = getLabeledPorts(launchConfig,
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
                    sourceProtocol = "tls";
                } else {
                    sourceProtocol = "https";
                }
            }
            listenersToReturn.add(new LoadBalancerListenerInfo(privatePort, sourcePort, sourceProtocol,
                    targetPort, proxyProtocolPorts.contains(privatePort.toString())));
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

    protected List<LoadBalancerTargetInput> getLoadBalancerTargetsV2(Service lbService) {
        if (!lbService.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return new ArrayList<>();
        }
        List<LoadBalancerTargetInput> targets = new ArrayList<>();
        List<? extends ServiceConsumeMap> lbLinks = objectManager.find(ServiceConsumeMap.class,
                SERVICE_CONSUME_MAP.REMOVED, null, SERVICE_CONSUME_MAP.SERVICE_ID, lbService.getId());
        for (ServiceConsumeMap lbLink : lbLinks) {
            if (lbLink.getState().equals(CommonStatesConstants.REMOVING)) {
                continue;
            }
            List<Service> consumedServices = new ArrayList<>();
            Service svc = objectManager.loadResource(Service.class, lbLink.getConsumedServiceId());
            consumedServices.add(svc);
            for (Service consumedService : consumedServices) {
                targets.add(new LoadBalancerTargetInput(consumedService, lbLink, jsonMapper));
            }
        }
        return targets;
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
    public LBConfigMetadataStyle generateLBConfigMetadataStyle(Service lbService) {
        Object lbConfigObj = DataAccessor.field(lbService, ServiceConstants.FIELD_LB_CONFIG, Object.class);
        if (lbConfigObj == null) {
            return null;
        }
        LbConfig lbConfig = null;
        if (lbConfigObj != null) {
            lbConfig = jsonMapper.convertValue(lbConfigObj, LbConfig.class);
        }
        // lb config can be set for lb and regular service (when it joins LB via selectors)
        // metadata gets set for both.
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

        return new LBConfigMetadataStyle(lbConfig.getPortRules(), lbConfig.getCertificateIds(),
                lbConfig.getDefaultCertificateId(),
                lbConfig.getConfig(), lbConfig.getStickinessPolicy(), serviceIdsToService,
                stackIdsToStack, certIdsToCert, lbService.getStackId(), false);
    }

    private static String getUuid(PortRule rule) {
        return String.format("%s_%s_%s_%s", rule.getSourcePort(), rule.getServiceId(), rule.getHostname(), rule.getPath());
    }

    @Override
    public LbConfig generateLBConfig(Service lbService) {
        if (!ServiceConstants.KIND_LOAD_BALANCER_SERVICE.equalsIgnoreCase(lbService.getKind())) {
            return null;
        }
        LbConfig lbConfig = DataAccessor.field(lbService, ServiceConstants.FIELD_LB_CONFIG, jsonMapper,
                LbConfig.class);
        if (lbConfig != null) {
            return lbConfig;
        }

        // generate lb config // the logic below is to support legacy APIs by programming all the rules to metadata
        List<? extends LoadBalancerListenerInfo> listeners = getListeners(lbService);
        if (listeners.isEmpty()) {
            return null;
        }
        // map listeners by sourcePort
        Map<Integer, LoadBalancerListenerInfo> portToListener = new HashMap<>();
        Map<Integer, List<Long>> sourcePortToServiceId = new HashMap<>();
        for (LoadBalancerListenerInfo listener : listeners) {
            portToListener.put(listener.getSourcePort(), listener);
            sourcePortToServiceId.put(listener.getSourcePort(), new ArrayList<Long>());
        }

        // get targets
        List<? extends LoadBalancerTargetInput> targets = getLoadBalancerTargetsV2(lbService);
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
                    PortRule portRule = new PortRule(hostname, path, listener.getSourcePort(), null,
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
                PortRule portRule = new PortRule("", "", sourcePort, null, PortRule.Protocol.valueOf(portToListener
                        .get(
                        sourcePort)
                        .getSourceProtocol()), String.valueOf(target.getService().getId()),
                        portToListener.get(sourcePort).getTargetPort(), null, null);

                if (!registeredRules.contains(getUuid(portRule))) {
                    registeredRules.add(getUuid(portRule));
                    rules.add(portRule);
                }
            }
        }

        List<Long> certs = getLoadBalancerCertIds(lbService);
        Long defaultCert = getLoadBalancerDefaultCertId(lbService);

        Object configObj = DataAccessor.field(lbService, ServiceConstants.FIELD_LOAD_BALANCER_CONFIG,
                Object.class);
        Map<String, Object> data = CollectionUtils.toMap(configObj);
        String config = "";
        LoadBalancerCookieStickinessPolicy policy = null;
        // global/default sections
        if (configObj != null) {
            policy = jsonMapper.convertValue(data.get(LoadBalancerConstants.FIELD_LB_COOKIE_POLICY),
                    LoadBalancerCookieStickinessPolicy.class);
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
        // proxy port for listeners
        for (Integer port : portToListener.keySet()) {
            if (portToListener.get(port).isProxyPort()) {
                config = String.format("%sfrontend %s\naccept-proxy\n", config, port.toString());
            }
        }

        return new LbConfig(config, rules, certs, defaultCert,
                policy);
    }

    public static class LoadBalancerListenerInfo {
        Integer privatePort;
        Integer sourcePort;
        Integer targetPort;
        String sourceProtocol;
        boolean proxyPort;

        public LoadBalancerListenerInfo(Integer privatePort, Integer sourcePort, String protocol, Integer targetPort,
                boolean proxyPort) {
            super();
            this.privatePort = privatePort;
            this.sourcePort = sourcePort;
            this.sourceProtocol = protocol;
            this.targetPort = targetPort;
            this.proxyPort = proxyPort;
        }

        // LEGACY code to support the case when private port is not defined
        public Integer getSourcePort() {
            return this.privatePort != null ? this.privatePort : this.sourcePort;
        }

        public Integer getTargetPort() {
            return targetPort;
        }

        public String getSourceProtocol() {
            return sourceProtocol;
        }

        public boolean isProxyPort() {
            return proxyPort;
        }

    }

}
