package io.cattle.platform.core.constants;

import io.cattle.platform.util.type.CollectionUtils;

import java.util.Set;

public class ProjectConstants {

    public static final String TYPE = "project";

    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_TYPE = "bearer ";
    public static final String CLIENT_ACCESS_KEY = "X-API-Client-Access-Key";
    public static final String NAME = "rancher";
    public static final String OAUTH_BASIC = "X-API-BEARER";
    public static final String OWNER = "owner";
    public static final String PROJECT_DEFAULT_NAME = "-Default";
    public static final String PROJECT_HEADER = "X-API-Project-Id";
    public static final String RANCHER_ID = "rancher_id";
    public static final String RANCHER_SEARCH_PROVIDER = "rancherIdentitySearchProvider";
    public static final String ROLES_HEADER = "X-API-Roles";

    public static final String DEFAULT_PROJECT_EXTERNAL_ID = "default";
    public static final String SYSTEM_PROJECT_EXTERNAL_ID = "cattle-system";

    public static final Set<String> SCOPES = CollectionUtils.set(RANCHER_ID);

}
