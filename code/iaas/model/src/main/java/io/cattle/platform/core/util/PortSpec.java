package io.cattle.platform.core.util;

import io.cattle.platform.core.constants.PortConstants;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class PortSpec {

    public static final String WRONG_FORMAT = "PortWrongFormat";
    public static final String INVALID_PUBLIC_PORT = "PortInvalidPublicPort";
    public static final String INVALID_PRIVATE_PORT = "PortInvalidPrivatePort";
    public static final String INVALID_PROTOCOL = "PortInvalidProtocol";

    private static final Pattern PATTERN = Pattern.compile("(([0-9]+):)?([0-9]+)(/(.*))?");
    private static final List<String> PROTOCOLS = Arrays.asList("tcp", "udp");

    int privatePort;
    String ipAddress;
    Integer publicPort;
    String protocol;

    public PortSpec() {
    }

    public PortSpec(Port port) {
        this.ipAddress = DataAccessor.fieldString(port, PortConstants.FIELD_BIND_ADDR);
        this.publicPort = port.getPublicPort();
        this.privatePort = port.getPrivatePort();
        this.protocol = port.getProtocol();
    }

    public PortSpec(String spec) {
        // format: ip:hostPort:containerPort | ip::containerPort | hostPort:containerPort | containerPort

        // Check for an IP address
        String[] parts = spec.split("\\]");
        String ipAddr = null;
        if (parts.length == 2) {
            // IPv6, right?
            if (!parts[0].startsWith("[")) {
                throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, WRONG_FORMAT);
            }
            ipAddr = parts[0].replace("[", "");
            spec = StringUtils.removeStart(parts[1], ":");
        } else if (StringUtils.countMatches(spec, ":") == 2) {
            parts = spec.split("\\:", 2);
            ipAddr = parts[0];
            spec = StringUtils.removeStart(parts[1], ":");
        }

        Matcher m = PATTERN.matcher(spec);

        if (!m.matches()) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, WRONG_FORMAT);
        }

        int privatePort = Integer.parseInt(m.group(3));
        Integer publicPort = m.group(2) == null ? null : Integer.parseInt(m.group(2));
        String protocol = m.group(5);

        if (privatePort <= 0 || privatePort > 65535) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, INVALID_PRIVATE_PORT);
        }

        if (publicPort != null && (publicPort <= 0 || publicPort > 65535)) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, INVALID_PUBLIC_PORT);
        }

        if (protocol == null) {
            protocol = "tcp";
        } else {
            if (!PROTOCOLS.contains(protocol)) {
                throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, INVALID_PROTOCOL);
            }
        }

        this.ipAddress = ipAddr;
        this.publicPort = publicPort;
        this.privatePort = privatePort;
        this.protocol = protocol;
    }

    public Integer getPublicPort() {
        return publicPort;
    }

    public int getPrivatePort() {
        return privatePort;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPrivatePort(int privatePort) {
        this.privatePort = privatePort;
    }

    public void setPublicPort(Integer publicPort) {
        this.publicPort = publicPort;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String toSpec() {
        String privatePortProto = this.privatePort + (this.protocol != null ? "/" + this.protocol : "");
        String publicPort = this.publicPort != null ? this.publicPort.toString() + ":" : "";
        String bindIP = "";
        if (StringUtils.isNotBlank(this.ipAddress)) {
            bindIP = this.ipAddress + ":";
        }
        return bindIP + publicPort + privatePortProto;
    }

    @Override
    public String toString() {
        return toSpec();
    }
}
