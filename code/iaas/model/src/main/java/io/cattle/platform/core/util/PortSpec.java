package io.cattle.platform.core.util;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortSpec {

    public static final String WRONG_FORMAT = "PortWrongFormat";
    public static final String INVALID_PUBLIC_PORT = "PortInvalidPublicPort";
    public static final String INVALID_PRIVATE_PORT = "PortInvalidPrivatePort";
    public static final String INVALID_PROTOCOL = "PortInvalidProtocol";

    private static final Pattern PATTERN = Pattern.compile("(([0-9]+):)?([0-9]+)(/(.*))?");
    private static final List<String> PROTOCOLS = Arrays.asList("tcp", "udp");

    int privatePort;
    Integer publicPort;
    String protocol;

    public PortSpec(String spec) {
        Matcher m = PATTERN.matcher(spec);

        if ( ! m.matches() ) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, WRONG_FORMAT);
        }

        int privatePort = Integer.parseInt(m.group(3));
        Integer publicPort = m.group(2) == null ? null : Integer.parseInt(m.group(2));
        String protocol = m.group(5);

        if ( privatePort <= 0 || privatePort > 65535 ) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, INVALID_PRIVATE_PORT);
        }

        if ( publicPort != null && ( publicPort <= 0 || publicPort > 65535 ) ) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, INVALID_PUBLIC_PORT);
        }

        if ( protocol == null ) {
            protocol = "tcp";
        }

        if ( ! PROTOCOLS.contains(protocol) ) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, INVALID_PROTOCOL);
        }

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

}
