package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class GithubConstants {



    public static final String NAME = "github";
    public static final String CONFIG = NAME + "config";

    public static final String ACCEPT = "Accept";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String ACCESSMODE_SETTING = "api.auth.github.access.mode";
    public static final String ALLOWED_IDENTITIES_SETTING = "api.auth.github.allowed.identities";
    public static final String APPLICATION_JSON = "application/json";
    public static final String AUTHORIZATION = "Authorization";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_ID_SETTING = "api.auth.github.client.id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String CLIENT_SECRET_SETTING = "api.auth.github.client.secret";
    public static final String GHE_API = "/api/v3";
    public static final String GITHUB_ACCESS_TOKEN = NAME + "access_token";
    public static final String GITHUB_API = "https://api.github.com";
    public static final String GITHUB_DEFAULT_HOSTNAME = "https://github.com";
    public static final String GITHUB_ERROR = "GitHubError";
    public static final String GITHUB_JWT = NAME + "jwt";
    public static final String GITHUB_REQUEST_CODE = "code";
    public static final String HOSTNAME = "hostname";
    public static final String HOSTNAME_SETTING = "api.github.domain";
    public static final String JWT_CREATION_FAILED = "FailedToMakeJWT";
    public static final String LOGIN = "login";
    public static final String NAME_FIELD = "name";
    public static final String ORG_SCOPE = NAME + "_org";
    public static final String PROFILE_PICTURE = "avatar_url";
    public static final String PROFILE_URL = "html_url";
    public static final String SCHEME_SETTING = "api.github.scheme";
    public static final String TEAM_SCOPE = NAME + "_team";
    public static final String USER_SCOPE = NAME + "_user";

    public static final DynamicBooleanProperty ALLOW_GITHUB_REDIRECT = ArchaiusUtil.getBoolean("api.allow.github.proxy");
    public static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString(CLIENT_ID_SETTING);
    public static final DynamicStringProperty GITHUB_CLIENT_SECRET = ArchaiusUtil.getString(CLIENT_SECRET_SETTING);
    public static final DynamicStringProperty GITHUB_HOSTNAME = ArchaiusUtil.getString(HOSTNAME_SETTING);
    public static final DynamicStringProperty SCHEME = ArchaiusUtil.getString(SCHEME_SETTING);
    public static final DynamicStringProperty ACCESS_MODE = ArchaiusUtil.getString(ACCESSMODE_SETTING);
    public static final DynamicStringProperty GITHUB_ALLOWED_IDENTITIES = ArchaiusUtil.getString(ALLOWED_IDENTITIES_SETTING);

    public static final Set<String> SCOPES = Collections.unmodifiableSet( new HashSet<>(Arrays.asList(
            USER_SCOPE, ORG_SCOPE, TEAM_SCOPE
    )));
    public static final String MANAGER = NAME + "Manager";
    public static final String SEARCH_PROVIDER = NAME + "Provider";
    public static final String TOKEN_CREATOR = NAME + "TokenCreator";
    public static final String TRANSFORMATION_HANDLER = NAME + "TransformationHandler";
    public static final String ACCOUNT_LOOKUP = NAME + "AccountLookUp";
    public static final String GITHUB_CLIENT = NAME + "Client";
}
