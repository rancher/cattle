package io.cattle.platform.core.util;

import io.cattle.platform.core.addon.LoadBalancerCookieStickinessPolicy;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.addon.TargetPortRule;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * this class it to support haproxy legacy API format
 */
public class LBMetadataUtil {
    
    public static final String LB_METADATA_KEY = "lb";
    
    public static class MetadataPortRule {
        public Integer source_port;
        public String protocol;
        public String path;
        public String hostname;
        public String service;
        public int target_port;
        public Integer priority = 0;
        public String backend_name;
        public String selector;

        public MetadataPortRule(PortRule portRule, Service service, Stack stack) {
            this.source_port = portRule.getSourcePort();
            this.protocol = portRule.getProtocol().name();
            this.path = portRule.getPath();
            this.hostname = portRule.getHostname();
            this.service = formatServiceName(service.getName(), stack.getName());
            this.target_port = portRule.getTargetPort();
            this.backend_name = portRule.getBackendName();
            this.priority = portRule.getPriority();
            this.selector = portRule.getSelector();
        }

        public MetadataPortRule(TargetPortRule portRule, String serviceName, String stackName) {
            this.path = portRule.getPath();
            this.hostname = portRule.getHostname();
            this.target_port = portRule.getTargetPort();
            this.service = formatServiceName(serviceName, stackName);
            this.backend_name = portRule.getBackendName();
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

        public int getTarget_port() {
            return target_port;
        }

        public void setTarget_port(int target_port) {
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

    public static class StickinessPolicy {
        String name;
        String cookie;
        String domain;
        Boolean indirect;
        Boolean nocache;
        Boolean postonly;
        String mode;

        public StickinessPolicy(LoadBalancerCookieStickinessPolicy policy) {
            super();
            this.name = policy.getName();
            this.cookie = policy.getCookie();
            this.domain = policy.getDomain();
            this.indirect = policy.getIndirect();
            this.nocache = policy.getNocache();
            this.postonly = policy.getPostonly();
            this.mode = policy.getMode().name();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCookie() {
            return cookie;
        }

        public void setCookie(String cookie) {
            this.cookie = cookie;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public Boolean getIndirect() {
            return indirect;
        }

        public void setIndirect(Boolean indirect) {
            this.indirect = indirect;
        }

        public Boolean getNocache() {
            return nocache;
        }

        public void setNocache(Boolean nocache) {
            this.nocache = nocache;
        }

        public Boolean getPostonly() {
            return postonly;
        }

        public void setPostonly(Boolean postonly) {
            this.postonly = postonly;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

    }

    public static class LBMetadata {
        public List<String> certs = new ArrayList<>();
        public String default_cert;
        public List<MetadataPortRule> port_rules = new ArrayList<>();
        public String config;
        public StickinessPolicy stickiness_policy;

        public LBMetadata(List<? extends PortRule> portRules, List<Long> certIds, Long defaultCertId,
                Map<Long, Service> services, Map<Long, Stack> stacks, Map<Long, Certificate> certificates,
                String config, StickinessPolicy stickinessPolicy) {
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
            for (PortRule portRule : portRules) {
                Long svcId = Long.valueOf(portRule.getServiceId());
                this.port_rules.add(new MetadataPortRule(portRule, services.get(svcId),
                        stacks.get(services.get(svcId).getStackId())));
            }
            this.config = config;
            this.stickiness_policy = stickinessPolicy;
        }

        public LBMetadata(List<? extends TargetPortRule> portRules, String serviceName, String stackName) {
            super();
            for (TargetPortRule portRule : portRules) {
                this.port_rules.add(new MetadataPortRule(portRule, serviceName, stackName));
            }
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

        public StickinessPolicy getStickiness_policy() {
            return stickiness_policy;
        }

        public void setStickiness_policy(StickinessPolicy stickiness_policy) {
            this.stickiness_policy = stickiness_policy;
        }

    }

    private static String formatServiceName(String serviceName, String stackName) {
        return String.format("%s/%s", stackName, serviceName);
    }
}

