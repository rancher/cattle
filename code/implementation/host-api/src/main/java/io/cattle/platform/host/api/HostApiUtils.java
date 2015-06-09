package io.cattle.platform.host.api;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicStringProperty;

public class HostApiUtils {

    public static final DynamicStringProperty HOST_API_PROXY_HOST = ArchaiusUtil.getString("host.api.proxy.host");
    public static final DynamicStringProperty HOST_API_PROXY_SCHEME = ArchaiusUtil.getString("host.api.proxy.scheme");
    
}
