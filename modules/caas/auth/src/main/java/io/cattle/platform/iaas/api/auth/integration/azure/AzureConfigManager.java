package io.cattle.platform.iaas.api.auth.integration.azure;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.core.constants.IdentityConstants;
import io.cattle.platform.core.dao.SettingDao;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AzureConfigManager extends AbstractNoOpResourceManager {

    private static final String CLIENT_ID = "clientId";
    private static final String TENANT_ID = "tenantId";
    private static final String ADMIN_USERNAME = "adminAccountUsername";
    private static final String ADMIN_PWD = "adminAccountPassword";

    AzureRESTClient client;
    AzureIdentityProvider azureIdentitySearchProvider;
    SettingDao settingsUtils;

    public AzureConfigManager(AzureRESTClient client, AzureIdentityProvider azureIdentitySearchProvider, SettingDao settingsUtils) {
        super();
        this.client = client;
        this.azureIdentitySearchProvider = azureIdentitySearchProvider;
        this.settingsUtils = settingsUtils;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        if (!StringUtils.equalsIgnoreCase(AzureConstants.CONFIG, request.getType())) {
            return null;
        }
        Map<String, Object> config = CollectionUtils.toMap(request.getRequestObject());

        return updateCurrentConfig(config);
    }

    public AzureConfig getCurrentConfig(Map<String, Object> config) {
        if (config == null){
            config = new HashMap<>();
        }
        boolean enabled = SecurityConstants.SECURITY.get();
        String clientId = AzureConstants.AZURE_CLIENT_ID.get();
        String accessMode = AzureConstants.ACCESS_MODE.get();
        String tenantId = AzureConstants.AZURE_TENANT_ID.get();
        String domain = AzureConstants.AZURE_DOMAIN.get();
        String adminAccountUsername = AzureConstants.AZURE_ADMIN_USERNAME.get();
        String adminAccountPassword = AzureConstants.AZURE_ADMIN_PASSWORD.get();

        if (config.get(SecurityConstants.ENABLED) != null) {
            enabled = (Boolean) config.get(SecurityConstants.ENABLED);
        }
        if (config.get(AbstractTokenUtil.ACCESSMODE) != null) {
            accessMode = (String) config.get(AbstractTokenUtil.ACCESSMODE);
        }
        if (config.get(CLIENT_ID) != null) {
            clientId = (String) config.get(CLIENT_ID);
        }
        if (config.get(TENANT_ID) != null) {
            tenantId = (String) config.get(TENANT_ID);
        }
        if (config.get(AzureConstants.DOMAIN) != null) {
            domain = (String) config.get(AzureConstants.DOMAIN);
        }
        if (config.get(ADMIN_USERNAME) != null) {
            adminAccountUsername = (String) config.get(ADMIN_USERNAME);
        }
        if (config.get(ADMIN_PWD) != null) {
            adminAccountPassword = (String) config.get(ADMIN_PWD);
        }

        return new AzureConfig(enabled, accessMode, tenantId, clientId, domain, adminAccountUsername, adminAccountPassword);
    }

    @Override
    public Object listSupport(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return getCurrentConfig(new HashMap<>());
    }

    public AzureConfig updateCurrentConfig(Map<String, Object> config) {

        settingsUtils.setValue(SecurityConstants.SECURITY_SETTING, config.get(SecurityConstants.ENABLED));

        if (config.get(CLIENT_ID) != null) {
            settingsUtils.setValue(AzureConstants.CLIENT_ID_SETTING, config.get(CLIENT_ID));
        }
        if (config.get(TENANT_ID) != null) {
            settingsUtils.setValue(AzureConstants.TENANT_ID_SETTING, config.get(TENANT_ID));
        }
        if (config.get(AzureConstants.DOMAIN) != null) {
            settingsUtils.setValue(AzureConstants.DOMAIN_SETTING, config.get(AzureConstants.DOMAIN));
        }
        if (config.get(ADMIN_USERNAME) != null) {
            settingsUtils.setValue(AzureConstants.ADMIN_USERNAME_SETTING, config.get(ADMIN_USERNAME));
        }
        if (config.get(ADMIN_PWD) != null) {
            settingsUtils.setValue(AzureConstants.ADMIN_PASSWORD_SETTING, config.get(ADMIN_PWD));
        }


        settingsUtils.setValue(AzureConstants.ACCESSMODE_SETTING, config.get(AbstractTokenUtil.ACCESSMODE));

        if (config.get(SecurityConstants.ENABLED) != null){
            settingsUtils.setValue(SecurityConstants.AUTH_PROVIDER_SETTING, AzureConstants.CONFIG);
        } else {
            settingsUtils.setValue(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
        }

        return getCurrentConfig(config);
    }

    public String getName() {
        return AzureConstants.MANAGER;
    }

    public String validateIdentities(List<Map<String, String>> identitiesGiven) {
        StringBuilder sb = new StringBuilder();
        List<Identity> identities = getIdentities(identitiesGiven);
        Iterator<Identity> identityIterator = identities.iterator();
        while (identityIterator.hasNext()){
            sb.append(identityIterator.next().getId().trim());
            if (identityIterator.hasNext()) sb.append(',');
        }
        return sb.toString();
    }

    private List<Identity> getIdentities(List<Map<String, String>> identitiesGiven) {
        if (identitiesGiven == null || identitiesGiven.isEmpty()){
            return new ArrayList<>();
        }
        if (!client.isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "AzureNotConfigured",
                    "Azure client id not set.", null);
        }

        List<Identity> identities = new ArrayList<>();
        for (Map<String, String> identity: identitiesGiven){
            String externalId = identity.get(IdentityConstants.EXTERNAL_ID);
            String externalIdType = identity.get(IdentityConstants.EXTERNAL_ID_TYPE);
            Identity gotIdentity = azureIdentitySearchProvider.getIdentity(externalId, externalIdType);
            if (gotIdentity == null) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidIdentity", "Invalid Identity", null);
            }
            identities.add(gotIdentity);
        }
        return identities;
    }
}
