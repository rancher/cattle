package io.cattle.platform.iaas.api.auth.github.constants;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class GithubConstants {


    public static final String TEAM_SCOPE = "github_team";
    public static final String ORG_SCOPE = "github_org";
    public static final String USER_SCOPE = "github_user";
    public static final String GITHUB_ACCESS_TOKEN = "access_token";
    public static final String GITHUB_JWT = "github_jwt";
    public static final String ACCOUNT_ID = "account_id";
    public static final String GITHUB_REQUEST_CODE = "code";
    public static final DynamicStringProperty GITHUB_HOSTNAME = ArchaiusUtil.getString("api.github.domain");
    public static final String GITHUB_DEFAULT_HOSTNAME = "github.com";
    public static final String GHE_API = "/api/v3";
    public static final String GITHUB_API = "api.github.com";
    public static final String SCHEME = "https://";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString("api.auth.github.client.id");
    public static final DynamicStringProperty GITHUB_CLIENT_SECRET = ArchaiusUtil.getString("api.auth.github.client.secret");
    public static final DynamicBooleanProperty ALLOW_GITHUB_REDIRECT = ArchaiusUtil.getBoolean("api.allow.github.proxy");
}
