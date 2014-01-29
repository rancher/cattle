package io.github.ibuildthecloud.dstack.iaas.config;

public interface ScopedConfig {

    public static final String API_URL = "api";
    public static final String CONFIG_URL = "config";
    public static final String STORAGE_URL = "storage";

    String getConfigUrl(Object context);

    String getStorageUrl(Object context);

    String getApiUrl(Object context);

    String getConfigUrl(Class<?> type, Object id);

    String getStorageUrl(Class<?> type, Object id);

    String getApiUrl(Class<?> type, Object id);

    String getUrl(Object context, String name);

    String getUrl(Class<?> type, Object id, String name);
}
