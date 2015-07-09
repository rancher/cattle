package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.iaas.api.auth.github.constants.GithubConstants;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.github.resource.GithubConfig;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class GithubConfigManager extends AbstractNoOpResourceManager {

    private static final String ENABLED = "enabled";
    private static final String ACCESSMODE = "accessMode";
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String ALLOWED_USERS = "allowedUsers";
    private static final String ALLOWED_ORGS = "allowedOrganizations";
    private static final String HOSTNAME = "hostname";
    private static final String SCHEME = "scheme";

    private static final String SECURITY_SETTING = "api.security.enabled";
    private static final String ACCESSMODE_SETTING = "api.auth.github.access.mode";
    private static final String CLIENT_ID_SETTING = "api.auth.github.client.id";
    private static final String CLIENT_SECRET_SETTING = "api.auth.github.client.secret";
    private static final String ALLOWED_USERS_SETTING = "api.auth.github.allowed.users";
    private static final String ALLOWED_ORGS_SETTING = "api.auth.github.allowed.orgs";
    private static final String HOSTNAME_SETTING = "api.github.domain";
    private static final String SCHEME_SETTING = "api.github.scheme";

    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean(SECURITY_SETTING);
    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString(CLIENT_ID_SETTING);
    private static final DynamicStringProperty ACCESS_MODE = ArchaiusUtil.getString(ACCESSMODE_SETTING);
    private static final DynamicStringProperty GITHUB_ALLOWED_USERS = ArchaiusUtil.getString(ALLOWED_USERS_SETTING);
    private static final DynamicStringProperty GITHUB_ALLOWED_ORGS = ArchaiusUtil.getString(ALLOWED_ORGS_SETTING);

    private JsonMapper jsonMapper;
    private ObjectManager objectManager;
    private GithubClient client;
    private GithubUtils githubUtils;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { GithubConfig.class };
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(GithubConstants.GITHUBCONFIG, request.getType())) {
            return null;
        }
        Map<String, Object> config = jsonMapper.convertValue(request.getRequestObject(), Map.class);
        changeSetting(HOSTNAME_SETTING, config.get(HOSTNAME));
        changeSetting(SCHEME_SETTING, config.get(SCHEME));
        changeSetting(SECURITY_SETTING, config.get(ENABLED));
        changeSetting(CLIENT_ID_SETTING, config.get(CLIENT_ID));
        if (config.get(CLIENT_SECRET) != null){
            changeSetting(CLIENT_SECRET_SETTING, config.get(CLIENT_SECRET));
        }
        changeSetting(ACCESSMODE_SETTING, config.get(ACCESSMODE));
        changeSetting(ALLOWED_USERS_SETTING, StringUtils.join(appendUserIds((List<String>) config.get(ALLOWED_USERS)), ","));
        changeSetting(ALLOWED_ORGS_SETTING, StringUtils.join(appendOrgIds((List<String>) config.get(ALLOWED_ORGS)), ","));
        return currentGithubConfig(config);
    }

    @SuppressWarnings("unchecked")
    private GithubConfig currentGithubConfig(Map<String, Object> config) {
        GithubConfig currentConfig = (GithubConfig) listInternal(null, null, null, null);
        Boolean enabled = currentConfig.getEnabled();
        if (config.get(ENABLED) != null) {
            enabled = (Boolean) config.get(ENABLED);
        }
        String accessMode = currentConfig.getAccessMode();
        if (config.get(ACCESSMODE) != null) {
            accessMode = (String) config.get(ACCESSMODE);
        }
        String hostname = currentConfig.getHostname();
        if (config.get(HOSTNAME) != null) {
            hostname = (String) config.get(HOSTNAME);
        }
        String scheme = currentConfig.getScheme();
        if (config.get(SCHEME) != null) {
            scheme = (String) config.get(SCHEME);
        }
        String clientId = currentConfig.getClientId();
        if (config.get(CLIENT_ID) != null) {
            clientId = (String) config.get(CLIENT_ID);
        }
        List<String> allowedUsers = currentConfig.getAllowedUsers();
        if (config.get(ALLOWED_USERS) != null) {
            allowedUsers = (List<String>) config.get(ALLOWED_USERS);
        }
        List<String> allowedOrgs = currentConfig.getAllowedOrganizations();
        if (config.get(ALLOWED_ORGS) != null) {
            allowedOrgs = (List<String>) config.get(ALLOWED_ORGS);
        }
        return new GithubConfig(enabled, accessMode, clientId, allowedUsers, allowedOrgs, hostname, scheme);
    }

    protected List<String> appendUserIds(List<String> usernames) {
        if (usernames == null) {
            return null;
        }
        List<String> appendedList = new ArrayList<>();

        for (String username : usernames) {
            GithubAccountInfo userInfo = client.getUserIdByName(username);
            if (userInfo == null) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidUsername", "Invalid username: " + username, null);
            }
            appendedList.add(userInfo.toString());
        }
        return appendedList;
    }

    protected List<String> appendOrgIds(List<String> orgs) {
        if (orgs == null) {
            return null;
        }
        List<String> appendedList = new ArrayList<>();
        for (String org : orgs) {
            GithubAccountInfo orgInfo = client.getOrgIdByName(org);
            if (orgInfo == null) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidOrganization", "Invalid organization: " + org, null);
            }
            appendedList.add(orgInfo.toString());
        }
        return appendedList;
    }

    private void changeSetting(String name, Object value) {
        if (name == null) {
            return;
        }
        Setting setting = objectManager.findOne(Setting.class, "name", name);
        if (value == null) {
            if (setting != null) {
                objectManager.delete(setting);
            } else{
                return;
            }
        } else {
            if (null == setting) {
                objectManager.create(Setting.class, "name", name, "value", value);
            } else {
                objectManager.setFields(setting, "value", value);
            }
        }
        DeferredUtils.defer(new Runnable() {

            @Override
            public void run() {
                ArchaiusUtil.refresh();
            }
        });
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        boolean enabled = SECURITY.get();
        String clientId = GITHUB_CLIENT_ID.get();
        String accessMode = ACCESS_MODE.get();
        String hostname = GithubConstants.GITHUB_HOSTNAME.get();
        String scheme = GithubConstants.SCHEME.get();
        List<String> allowedUsers = getAccountNames(fromCommaSeparatedString(GITHUB_ALLOWED_USERS.get()));
        List<String> allowedOrgs = getAccountNames(fromCommaSeparatedString(GITHUB_ALLOWED_ORGS.get()));
        return new GithubConfig(enabled, accessMode, clientId, allowedUsers, allowedOrgs, hostname, scheme);
    }

    private List<String> getAccountNames(List<String> accountInfos) {
        if (accountInfos == null) {
            return null;
        }
        return Lists.transform(accountInfos, new Function<String, String>() {

            @Override
            public String apply(String accountInfo) {
                String[] accountInfoArr = accountInfo.split("[:]");
                return accountInfoArr[0];
            }

        });
    }

    protected List<String> fromCommaSeparatedString(String string) {
        if (StringUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        List<String> strings = new ArrayList<String>();
        String[] splitted = string.split(",");
        for (int i = 0; i < splitted.length; i++) {
            String element = splitted[i].trim();
            strings.add(element);
        }
        return strings;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Inject
    public void setGithubClient(GithubClient client) {
        this.client = client;
    }

    @Inject
    public void setGithubUtils(GithubUtils githubUtils) {
        this.githubUtils = githubUtils;
    }

}
