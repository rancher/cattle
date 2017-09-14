package io.cattle.platform.core.util;

import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortRangeSpec {
    private static final Pattern PATTERN = Pattern.compile("(([0-9]+)-)([0-9]+)");

    public static final String WRONG_FORMAT = "PortRangeWrongFormat";
    public static final String INVALID_START_PORT = "PortInvalidStartPort";
    public static final String INVALID_END_PORT = "PortInvalidEndPort";

    int startPort;
    int endPort;

    public PortRangeSpec(String spec) {
        Matcher m = PATTERN.matcher(spec);

        if (!m.matches()) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, WRONG_FORMAT);
        }

        int endPort = Integer.parseInt(m.group(3));
        Integer startPort = m.group(2) == null ? null : Integer.parseInt(m.group(2));

        if (endPort <= 0 || endPort > 65535) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, INVALID_END_PORT);
        }

        if (startPort <= 0 || startPort > 65535) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, INVALID_START_PORT);
        }

        this.startPort = startPort;
        this.endPort = endPort;
    }

    public int getStartPort() {
        return startPort;
    }

    public int getEndPort() {
        return endPort;
    }
}