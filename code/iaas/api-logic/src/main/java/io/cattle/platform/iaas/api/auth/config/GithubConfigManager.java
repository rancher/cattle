package io.cattle.platform.iaas.api.auth.config;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.iaas.api.auth.github.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.github.GithubClient;
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

    private static final String GITHUB_CONFIG = "githubconfig";

    private static final DynamicBooleanProperty SECURITY = ArchaiusUtil.getBoolean("api.security.enabled");
    private static final DynamicStringProperty GITHUB_CLIENT_ID = ArchaiusUtil.getString("api.auth.github.client.id");
    private static final DynamicStringProperty GITHUB_ALLOWED_USERS = ArchaiusUtil.getString("api.auth.github.allow.users");
    private static final DynamicStringProperty GITHUB_ALLOWED_ORGS = ArchaiusUtil.getString("api.auth.github.allow.orgs");

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
        Map<String, Object> config = jsonMapper.convertValue(request.getRequestBodyObject(), Map.class);
        if (null != config.get("enabled")) {
            createOrUpdateSetting("api.security.enabled", config.get("enabled"));
        }
        if (null != config.get("clientId")) {
            createOrUpdateSetting("api.auth.github.client.id", config.get("clientId"));
        }
        if (null != config.get("clientSecret")) {
            createOrUpdateSetting("api.auth.github.client.secret", config.get("clientSecret"));
        }
        if (null != config.get("allowUsers")) {
            List<String> userList;
            try {
                userList = appendUserIds((List<String>) config.get("allowUsers"));
            } catch (ClassCastException e) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "allowedUsers should be a list");
            }
            createOrUpdateSetting("api.auth.github.allow.users", toCommaSeparatedString(userList));
        }
        if (null != config.get("allowOrganizations")) {
            List<String> orgList;
            try {
                orgList = appendOrgIds((List<String>) config.get("allowOrganizations"));
            } catch (ClassCastException e) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "allowedOrganizations should be a list");
            }
            createOrUpdateSetting("api.auth.github.allow.orgs", toCommaSeparatedString(orgList));
        }

        return new Object();
    }

    protected List<String> appendUserIds(List<String> usernames) {
        if(usernames == null) {
            return null;
        }
        return Lists.transform(usernames, new Function<String, String> () {

            @Override
            public String apply(String username) {
                GithubAccountInfo userInfo = client.getUserIdByName(username);
                if(userInfo == null) {
                    throw new RuntimeException("Invalid username: " + username);
                }
                return userInfo.toString();
            }
            
        });
    }
    
    protected List<String> appendOrgIds(List<String> orgs) {
        if(orgs == null) {
            return null;
        }
        return Lists.transform(orgs, new Function<String, String> () {

            @Override
            public String apply(String org) {
                GithubAccountInfo orgInfo = client.getOrgIdByName(org);
                if(orgInfo == null) {
                    throw new RuntimeException("Invalid organization: " + org);
                }
                return orgInfo.toString();
            }
            
        });
    }
    
    private void createOrUpdateSetting(String name, Object value) {
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
        if(accountInfos == null) {
            return null;
        }
        return Lists.transform(accountInfos, new Function<String, String> () {

            @Override
            public String apply(String accountInfo) {
                String[] accountInfoArr = accountInfo.split("[:]");
                if(accountInfoArr.length <= 1) {
                    throw new RuntimeException("Invalid account info");
                }
                return accountInfoArr[0];
            }
            
        });
    }

    protected List<String> fromCommaSeparatedString(String string) {
        if(StringUtils.isEmpty(string)) {
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

    protected String toCommaSeparatedString(List<String> list) {
        if (null == list) {
            return null;
        }
        if(list.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String obj : list) {
            sb.append(obj);
            sb.append(",");
        }
        return sb.substring(0, sb.length() - 1);
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