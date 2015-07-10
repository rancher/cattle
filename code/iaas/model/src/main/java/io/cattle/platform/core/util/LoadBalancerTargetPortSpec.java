package io.cattle.platform.core.util;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoadBalancerTargetPortSpec {
    public static final String WRONG_FORMAT = "PortWrongFormat";
    public static final String INVALID_PORT = "PortInvalidPublicPort";
    public static final String DEFAULT = "default";

    private static final Pattern PATTERN = Pattern.compile("([0-9]+)(:(.*)?)?");

    int port;
    String domain = DEFAULT;
    String path = DEFAULT;

    public LoadBalancerTargetPortSpec() {
    }

    public LoadBalancerTargetPortSpec(String spec) {
        Matcher m = PATTERN.matcher(spec);

        if (!m.matches()) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, WRONG_FORMAT);
        }

        setPort(m);
        setHostAndDomain(m);
    }

    public LoadBalancerTargetPortSpec(int port) {
        this.port = port;
    }

    protected void setHostAndDomain(Matcher m) {
        String domainPath = m.group(3) != null ? m.group(3) : "";
        if (domainPath.length() > 0) {
            int slashIndex = domainPath.indexOf("/");
            if (slashIndex == -1) {
                this.domain = domainPath;
            } else if (slashIndex == 0) {
                this.path = domainPath;
            } else {
                this.domain = domainPath.substring(0, slashIndex);
                this.path = domainPath.substring(slashIndex, domainPath.length());
            }
        }

        // TODO - add domain and path validation
    }

    protected void setPort(Matcher m) {
        Integer port = Integer.parseInt(m.group(1));
        if (port != null && (port <= 0 || port > 65535)) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, INVALID_PORT);
        }

        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
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
}
