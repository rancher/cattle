package io.cattle.platform.core.constants;

import org.bouncycastle.jce.X509Principal;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;

public class CertificateConstants {

    public static final String CA = "CA";
    public static final String SERVER = "server";
    public static final String CLUSTER = "cluster";
    public static final String CLIENT = "client";

    public static final X509Principal CERT_ISSUER = new X509Principal("C=US, ST=CA, L=Cupertino, O=Rancher");
    public static final DynamicLongProperty EXPIRATION_MILLIS =
            DynamicPropertyFactory.getInstance().getLongProperty("ssl.cert.expiration", 1000L * 60 * 60 * 24 * 365);
}
