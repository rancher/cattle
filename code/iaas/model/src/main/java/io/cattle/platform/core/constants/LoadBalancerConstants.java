package io.cattle.platform.core.constants;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancerConstants {
    public static final String FIELD_LB_APP_COOKIE_POLICY = "appCookieStickinessPolicy";
    public static final String FIELD_LB_COOKIE_POLICY = "lbCookieStickinessPolicy";
    public static final String FIELD_LB_SERVICE_ID = "serviceId";
    public static final String FIELD_LB_TARGET_PORTS = "ports";
    public static final String FIELD_LB_CERTIFICATE_IDS = "certificateIds";
    public static final String FIELD_LB_DEFAULT_CERTIFICATE_ID = "defaultCertificateId";
    public static final String FIELD_LB_DEFAULTS = "defaults";

    public static final List<String> DEFATULTS;
    static
    {
        DEFATULTS = new ArrayList<>();
        DEFATULTS.add("log global");
        DEFATULTS.add("mode tcp");
        DEFATULTS.add("option tcplog");
        DEFATULTS.add("option dontlognull");
        DEFATULTS.add("option redispatch");
        DEFATULTS.add("option http-server-close");
        DEFATULTS.add("option forwardfor");
        DEFATULTS.add("retries 3");
        DEFATULTS.add("timeout connect 5000");
        DEFATULTS.add("timeout client 50000");
        DEFATULTS.add("timeout server 50000");
        DEFATULTS.add("balance roundrobin");
    }
}
