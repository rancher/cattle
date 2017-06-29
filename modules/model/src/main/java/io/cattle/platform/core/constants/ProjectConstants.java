package io.cattle.platform.core.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ProjectConstants {

    public static final String TYPE = "project";
    public static final String PROJECT_DEFAULT_NAME = "-Default";

    public static final String RANCHER_ID = "rancher_id";
    public static final String NAME = "rancher";

    public static final String OWNER = "owner";


    public static final String AUTH_HEADER = "Authorization";
    public static final String OAUTH_BASIC = "X-API-BEARER";
    public static final String AUTH_TYPE = "bearer ";
    public static final String PROJECT_HEADER = "X-API-Project-Id";
    public static final String ROLES_HEADER = "X-API-Roles";
    public static final String CLIENT_ACCESS_KEY = "X-API-Client-Access-Key";
    public static final String RANCHER_SEARCH_PROVIDER = "rancherIdentitySearchProvider";
    public static final Set<String> SCOPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            RANCHER_ID
    )));
}
