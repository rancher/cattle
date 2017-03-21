package io.cattle.platform.core.util;

import io.cattle.platform.core.addon.LoadBalancerCookieStickinessPolicy;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/*
 * this class it to support haproxy legacy API format
 */
public class LBMetadataUtil {

    public static class MetadataPortRule {
        public Integer source_port;
        public String protocol;
        public String path;
        public String hostname;
        public String service;
        public Integer target_port;
        public Integer priority = 0;
        public String backend_name;
        public String selector;

        public MetadataPortRule(PortRule portRule, Service service, Stack stack) {
            this.source_port = portRule.getSourcePort();
            if (portRule.getProtocol() != null) {
                this.protocol = portRule.getProtocol().name();
            }
            this.path = portRule.getPath();
            this.hostname = portRule.getHostname();
            if (service != null) {
                if (stack != null) {
                    this.service = formatServiceName(service.getName(), stack.getName());
                } else {
                    this.service = service.getName();
                }
            }
            this.target_port = portRule.getTargetPort();
            this.backend_name = portRule.getBackendName();
            this.priority = portRule.getPriority();
            this.selector = portRule.getSelector();
        }

        public String getSelector() {
            return selector;
        }

        public void setSelector(String selector) {
            this.selector = selector;
        }

        public Integer getSource_port() {
            return source_port;
        }

        public void setSource_port(Integer source_port) {
            this.source_port = source_port;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public Integer getTarget_port() {
            return target_port;
        }

        public void setTarget_port(Integer target_port) {
            this.target_port = target_port;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }

        public String getBackend_name() {
            return backend_name;
        }

        public void setBackend_name(String backend_name) {
            this.backend_name = backend_name;
        }
    }

    public static class LBConfigMetadataStyle {
        public List<String> certs = new ArrayList<>();
        public String default_cert;
        public List<MetadataPortRule> port_rules = new ArrayList<>();
        public String config;
        public LoadBalancerCookieStickinessPolicy stickiness_policy;

        public LBConfigMetadataStyle() {
            super();
        }

        public LBConfigMetadataStyle(List<? extends PortRule> portRules, List<Long> certIds, Long defaultCertId,
                String config, LoadBalancerCookieStickinessPolicy stickinessPolicy, Map<Long, Service> services,
                Map<Long, Stack> stacks, Map<Long, Certificate> certificates, Long serviceStackId, boolean dropStackName) {
            super();
            if (certIds != null) {
                for (Long certId : certIds) {
                    Certificate cert = certificates.get(certId);
                    if (cert != null) {
                        this.certs.add(cert.getName());
                    }
                }
            }
            if (defaultCertId != null) {
                Certificate defaultCert = certificates.get(defaultCertId);
                if (defaultCert != null) {
                    this.default_cert = defaultCert.getName();
                }
            }
            if (portRules != null) {
                for (PortRule portRule : portRules) {
                    if (portRule.getServiceId() != null) {
                        Long svcId = Long.valueOf(portRule.getServiceId());
                        Service targetService = services.get(svcId);
                        if (targetService == null) {
                            continue;
                        }
                        Stack targetStack = stacks.get(targetService.getStackId());
                        if (targetStack == null) {
                            continue;
                        }
                        if (dropStackName && targetStack.getId().equals(serviceStackId)) {
                            this.port_rules.add(new MetadataPortRule(portRule, targetService,
                                    null));
                        } else {
                            this.port_rules.add(new MetadataPortRule(portRule, targetService,
                                    targetStack));
                        }
                    } else {
                        this.port_rules.add(new MetadataPortRule(portRule, null, null));
                    }
                }
            }

            this.config = config;
            this.stickiness_policy = stickinessPolicy;
        }

        public List<String> getCerts() {
            return certs;
        }

        public void setCerts(List<String> certs) {
            this.certs = certs;
        }

        public String getDefault_cert() {
            return default_cert;
        }

        public void setDefault_cert(String default_cert) {
            this.default_cert = default_cert;
        }

        public List<MetadataPortRule> getPort_rules() {
            return port_rules;
        }

        public void setPort_rules(List<MetadataPortRule> routing_rules) {
            this.port_rules = routing_rules;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }

        public LoadBalancerCookieStickinessPolicy getStickiness_policy() {
            return stickiness_policy;
        }

        public void setStickiness_policy(LoadBalancerCookieStickinessPolicy stickiness_policy) {
            this.stickiness_policy = stickiness_policy;
        }

    }

    private static String formatServiceName(String serviceName, String stackName) {
        if (StringUtils.isEmpty(stackName)) {
            return serviceName;
        }
        return String.format("%s/%s", stackName, serviceName);
    }
}

