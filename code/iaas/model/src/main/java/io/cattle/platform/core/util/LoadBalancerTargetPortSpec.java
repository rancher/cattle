package io.cattle.platform.core.util;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import org.apache.commons.lang.StringUtils;

/**
 * This class is to parse the port specs defined for the load balancer, in format
 * [[[domain:][sourcePort][/path]=][targetPort]]
 * 
 * example.com:80/path=81
 * example.com
 * example.com:80
 * example.com:80/path
 * example.com:80/path=81
 * example.com:80=81
 * example.com/path
 * example.com/path=81
 * example.com=81
 * 80/path
 * 80/path=81
 * 80=81
 * /path
 * /path=81
 * 81
 */
public class LoadBalancerTargetPortSpec {
    public static final String WRONG_FORMAT = "InvalidPort";
    public static final String DEFAULT = "default";

    Integer port;
    String domain = DEFAULT;
    String path = DEFAULT;
    Integer sourcePort;

    public LoadBalancerTargetPortSpec() {
    }

    public LoadBalancerTargetPortSpec(String input) {
        String targetPort = null;
        String domain = null;
        String path = null;
        String sourcePort = null;

        if (input.isEmpty()) {
            return;
        }

        if (input.split("=").length > 2 || input.split(":").length > 2) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, WRONG_FORMAT);
        }

        try {
            // target port assignment
            String[] splittedByEqual = input.split("=");
            if (splittedByEqual.length > 1) {
                targetPort = splittedByEqual[1];
                input = splittedByEqual[0];
            } else {
                try {
                    Integer.valueOf(splittedByEqual[0]);
                    targetPort = splittedByEqual[0];
                    return;
                } catch (NumberFormatException ex) {
                }
            }
            
            // path assignment
            String[] splittedBySlash = StringUtils.split(input, "/", 2);
            if (splittedBySlash.length > 1) {
                path = "/" + splittedBySlash[1];
                input = splittedBySlash[0];
            } else if (input.startsWith("/")) {
                path = input;
                return;
            }

            // domain name assignment
            String[] splittedByColon = StringUtils.split(input, ":");
            if (splittedByColon.length > 1) {
                domain = splittedByColon[0];
                sourcePort = splittedByColon[1];
            } else {
                try {
                    Integer.valueOf(splittedByColon[0]);
                    sourcePort = splittedByColon[0];
                } catch (NumberFormatException ex) {
                    domain = splittedByColon[0];
                }
            }

        } finally {
            if (!StringUtils.isEmpty(domain)) {
                this.domain = domain;
            }

            if (!StringUtils.isEmpty(path)) {
                this.path = path;
            }

            if (!StringUtils.isEmpty(targetPort)) {
                this.port = validatePort(targetPort);
            }
            if (!StringUtils.isEmpty(sourcePort)) {
                this.sourcePort = validatePort(sourcePort);
            }
        }
    }

    protected Integer validatePort(String portStr) {
        Integer port = null;
        try {
            port = Integer.valueOf(portStr);
        } catch (NumberFormatException ex) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, WRONG_FORMAT);
        }
        if (port != null && (port <= 0 || port > 65535)) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, WRONG_FORMAT);
        }
        return port;
    }

    public LoadBalancerTargetPortSpec(int targetPort, int sourcePort) {
        this.port = targetPort;
        this.sourcePort = sourcePort;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }
}
