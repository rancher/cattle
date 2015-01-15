package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.deferred.util.DeferredUtils;
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

import java.io.IOException;
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

    private static final String GITHUB_CONFIG = "githubconfig";

    private static final String SECURITY_SETTING = "api.security.enabled";
    private static final String CLIENT_ID_SETTING = "api.auth.github.client.id";
    private static final String ALLOWED_USERS_SETTING = "api.auth.github.allowed.users";
    private static final String ALLOWED_ORGS_SETTING = "api.auth.github.allowed.orgs";

    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean(SECURITY_SETTING);
    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString(CLIENT_ID_SETTING);
    private static final DynamicStringProperty GITHUB_ALLOWED_USERS = ArchaiusUtil.getString(ALLOWED_USERS_SETTING);
    private static final DynamicStringProperty GITHUB_ALLOWED_ORGS = ArchaiusUtil.getString(ALLOWED_ORGS_SETTING);

    private JsonMapper jsonMapper;
    private ObjectManager objectManager;
    private GithubClient client;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { GithubConfig.class };
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!StringUtils.equals(GITHUB_CONFIG, request.getType())) {
            return null;
        }
        Map<String, Object> config = jsonMapper.convertValue(request.getRequestObject(), Map.class);
        createOrUpdateSetting(SECURITY_SETTING, config.get("enabled"));
        createOrUpdateSetting(CLIENT_ID_SETTING, config.get("clientId"));
        createOrUpdateSetting("api.auth.github.client.secret", config.get("clientSecret"));
        try {
            createOrUpdateSetting(ALLOWED_USERS_SETTING, StringUtils.join(appendUserIds((List<String>) config.get("allowedUsers")), ","));
            createOrUpdateSetting(ALLOWED_ORGS_SETTING, StringUtils.join(appendOrgIds((List<String>) config.get("allowedOrganizations")), ","));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Object();
    }

    protected List<String> appendUserIds(List<String> usernames) throws IOException {
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

    protected List<String> appendOrgIds(List<String> orgs) throws IOException {
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

    private void createOrUpdateSetting(String name, Object value) {
        if (null == value || null == name) {
            return;
        }
        Setting setting = objectManager.findOne(Setting.class, "name", name);
        if (null == setting) {
            objectManager.create(Setting.class, "name", name, "value", value);
        } else {
            objectManager.setFields(setting, "value", value);
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
        List<String> allowedUsers = getAccountNames(fromCommaSeparatedString(GITHUB_ALLOWED_USERS.get()));
        List<String> allowedOrgs = getAccountNames(fromCommaSeparatedString(GITHUB_ALLOWED_ORGS.get()));
        return new GithubConfig(enabled, clientId, allowedUsers, allowedOrgs);
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
            return new ArrayList<String>();
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

}