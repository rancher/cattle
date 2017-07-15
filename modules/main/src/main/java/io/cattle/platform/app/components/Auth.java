package io.cattle.platform.app.components;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.schema.FileSchemaFactory;
import io.cattle.platform.api.schema.builder.SchemaFactoryBuilder;
import io.cattle.platform.core.util.SettingsUtils;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.AchaiusPolicyOptionsFactory;
import io.cattle.platform.iaas.api.auth.AuthorizationProvider;
import io.cattle.platform.iaas.api.auth.dynamic.DynamicSchemaAuthorizationProvider;
import io.cattle.platform.iaas.api.auth.identity.IdentityManager;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.identity.TokenResourceManager;
import io.cattle.platform.iaas.api.auth.impl.AgentQualifierAuthorizationProvider;
import io.cattle.platform.iaas.api.auth.impl.ApiAuthenticator;
import io.cattle.platform.iaas.api.auth.impl.DefaultAuthorizationProvider;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureConfig;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureConfigManager;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureIdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureRESTClient;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureTokenCreator;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureTokenUtil;
import io.cattle.platform.iaas.api.auth.integration.external.ExternalServiceAuthProvider;
import io.cattle.platform.iaas.api.auth.integration.external.ExternalServiceTokenUtil;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.iaas.api.auth.integration.interfaces.IdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenCreator;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenUtil;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.AdminAuthLookUp;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.BasicAuthImpl;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.RancherIdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.TokenAccountLookup;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.TokenAuthLookup;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPConfig;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPConfigManager;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPIdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPTokenCreator;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPUtils;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADConfig;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADConfigManager;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADIdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADTokenCreator;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADTokenUtils;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthConfig;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthConfigManager;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthTokenCreator;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthTokenUtils;
import io.cattle.platform.iaas.api.auth.integration.register.RegistrationTokenAccountLookup;
import io.cattle.platform.iaas.api.auth.projects.ProjectMemberResourceManager;
import io.cattle.platform.iaas.api.auth.projects.ProjectResourceManager;
import io.cattle.platform.iaas.api.auth.projects.SetProjectMembersActionHandler;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRouter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Auth {

    Common c;
    Framework f;
    DataAccess d;

    ExternalServiceTokenUtil externalServiceTokenUtil;
    ExternalServiceAuthProvider externalServiceAuthProvider;

    Map<Class<?>, ResourceManager> resourceManagers = new HashMap<>();

    List<IdentityProvider> identityProviders = new ArrayList<>();
    List<TokenCreator> tokenCreators = new ArrayList<>();
    List<TokenUtil> tokenUtils = new ArrayList<>();
    List<AccountLookup> accountLookups = new ArrayList<>();
    List<AuthorizationProvider> authorizationProviders = new ArrayList<>();

    SettingsUtils settingsUtils;

    public Auth(Framework framework, Common common, DataAccess dataAccess) throws IOException {
        this.f = framework;
        this.c = common;
        this.d = dataAccess;

        this.settingsUtils = new SettingsUtils(framework.objectManager);

        setupAD();
        setupAzure();
        setupLocal();
        setupOpenLDAP();
        setupExternal();

        setupAccountLookup();
        setupAuthorizationProvider();
        setupChildSchemas();
        setupV1Schemas();
    }

    private void setupAuthorizationProvider() {
        AchaiusPolicyOptionsFactory optionsFactory = new AchaiusPolicyOptionsFactory();
        AgentQualifierAuthorizationProvider agentQualifierAuthorizationProvider = new AgentQualifierAuthorizationProvider(
                optionsFactory,
                c.locator,
                f.processManager);

        DefaultAuthorizationProvider defaultAuthorizationProvider = new DefaultAuthorizationProvider(optionsFactory,
                d.authDao,
                d.accountDao,
                c.schemaFactories);

        DynamicSchemaAuthorizationProvider dynamicProvider = new DynamicSchemaAuthorizationProvider(d.dynamicSchemaDao,
                f.schemaJsonMapper,
                defaultAuthorizationProvider);


        // Must be first
        authorizationProviders.add(agentQualifierAuthorizationProvider);
        authorizationProviders.add(dynamicProvider);
    }

    private void setupAccountLookup() {
        AdminAuthLookUp adminAuthLookup = new AdminAuthLookUp(d.authDao);
        TokenAuthLookup tokenAuthLookup = new TokenAuthLookup(tokenUtils, externalServiceTokenUtil);
        BasicAuthImpl basicAuth = new BasicAuthImpl(d.authDao, adminAuthLookup, f.objectManager, tokenAuthLookup);
        RegistrationTokenAccountLookup registerLookup = new RegistrationTokenAccountLookup(c.registrationAuthTokenManager);
        TokenAccountLookup tokenAccountLookup = new TokenAccountLookup(d.authDao);

        accountLookups.add(tokenAccountLookup);
        accountLookups.add(tokenAuthLookup);
        accountLookups.add(registerLookup);
        accountLookups.add(basicAuth);
        accountLookups.add(adminAuthLookup);
    }

    private void setupAD() {
        ADTokenUtils adTokenUtils = new ADTokenUtils(
                d.authDao,
                c.tokenService,
                d.authTokenDao,
                f.objectManager,
                settingsUtils,
                d.accountDao);
        ADIdentityProvider adIdentityProvider = new ADIdentityProvider(adTokenUtils, f.executorService, d.authTokenDao);
        ADConfigManager adConfigResourceManager = new ADConfigManager(settingsUtils, f.jsonMapper, adIdentityProvider);
        ADTokenCreator adTokenCreator = new ADTokenCreator(adIdentityProvider, adTokenUtils);

        identityProviders.add(adIdentityProvider);
        tokenCreators.add(adTokenCreator);
        tokenUtils.add(adTokenUtils);
        resourceManagers.put(ADConfig.class, adConfigResourceManager);
    }

    private void setupAzure() {
        AzureTokenUtil azureTokenUtils = new AzureTokenUtil(
                d.authDao,
                c.tokenService,
                d.authTokenDao,
                f.objectManager,
                settingsUtils,
                d.accountDao);
        AzureRESTClient azureRestClient = new AzureRESTClient(f.jsonMapper, azureTokenUtils);
        AzureTokenCreator azureTokenCreator = new AzureTokenCreator(azureTokenUtils, azureRestClient);
        AzureIdentityProvider azureIdentityProvider = new AzureIdentityProvider(azureRestClient, azureTokenUtils, d.authTokenDao, azureTokenCreator);
        AzureConfigManager azureConfigManager = new AzureConfigManager(azureRestClient, azureIdentityProvider, settingsUtils);

        identityProviders.add(azureIdentityProvider);
        tokenCreators.add(azureTokenCreator);
        tokenUtils.add(azureTokenUtils);
        resourceManagers.put(AzureConfig.class, azureConfigManager);
    }

    private void setupLocal() {
        LocalAuthTokenUtils localAuthTokenUtils = new LocalAuthTokenUtils(
                d.authDao,
                c.tokenService,
                d.authTokenDao,
                f.objectManager,
                settingsUtils,
                d.accountDao);
        RancherIdentityProvider rancherIdentityProvider = new RancherIdentityProvider(d.authDao, f.idFormatter);
        LocalAuthTokenCreator localAuthTokenCreator = new LocalAuthTokenCreator(d.authDao, localAuthTokenUtils, rancherIdentityProvider);
        LocalAuthConfigManager localAuthConfigManager = new LocalAuthConfigManager(d.passwordDao, settingsUtils, f.jsonMapper);

        identityProviders.add(rancherIdentityProvider);
        tokenCreators.add(localAuthTokenCreator);
        tokenUtils.add(localAuthTokenUtils);
        resourceManagers.put(LocalAuthConfig.class, localAuthConfigManager);
    }

    private void setupOpenLDAP() {
        OpenLDAPUtils openLDAPUtils = new OpenLDAPUtils(
                d.authDao,
                c.tokenService,
                d.authTokenDao,
                f.objectManager,
                settingsUtils,
                d.accountDao);
        OpenLDAPIdentityProvider openLDAPIdentityProvider = new OpenLDAPIdentityProvider(openLDAPUtils, d.authTokenDao, f.executorService);
        OpenLDAPTokenCreator openLDAPTokenCreator = new OpenLDAPTokenCreator(openLDAPIdentityProvider, openLDAPUtils);
        OpenLDAPConfigManager openLDAPConfigManager = new OpenLDAPConfigManager(settingsUtils, openLDAPIdentityProvider);

        identityProviders.add(openLDAPIdentityProvider);
        tokenCreators.add(openLDAPTokenCreator);
        tokenUtils.add(openLDAPUtils);
        resourceManagers.put(OpenLDAPConfig.class, openLDAPConfigManager);
    }

    private void setupExternal() {
        externalServiceTokenUtil = new ExternalServiceTokenUtil(
                d.authDao,
                c.tokenService,
                d.authTokenDao,
                f.objectManager,
                settingsUtils,
                d.accountDao);

        externalServiceAuthProvider = new ExternalServiceAuthProvider(
                f.jsonMapper,
                c.tokenService,
                externalServiceTokenUtil,
                d.authTokenDao);

        tokenUtils.add(externalServiceTokenUtil);
    }

    public void injectAuth(ApiRouter router) {
        IdentityManager identityManager = new IdentityManager(
                identityProviders,
                f.executorService,
                externalServiceAuthProvider);

        ProjectMemberResourceManager projectMemberResourceManager = new ProjectMemberResourceManager(c.support, d.authDao, identityManager);
        ProjectResourceManager projectResourceManager = new ProjectResourceManager(c.support, d.authDao, projectMemberResourceManager, d.accountDao);
        TokenResourceManager tokenResourceManager = new TokenResourceManager(d.authTokenDao, identityManager, externalServiceAuthProvider, f.eventService,
                tokenCreators);

        router.resourceManager(Token.class, tokenResourceManager);
        router.resourceManager("project", projectResourceManager);
        router.resourceManager("projectMember", projectMemberResourceManager);
        router.resourceManager(Identity.class, identityManager);
        router.action("account.setmembers", new SetProjectMembersActionHandler(d.authDao, projectMemberResourceManager));


        resourceManagers.forEach((type, rm) -> {
            router.resourceManager(type, rm);
        });

        ApiAuthenticator apiAuthenticator = router.getHandlers().stream()
                .filter((x) -> (x instanceof ApiAuthenticator))
                .map((x) -> (ApiAuthenticator)x)
                .findFirst().orElseThrow(() -> new IllegalStateException("Failed to find API Authenticator"));

        apiAuthenticator.getAccountLookups().addAll(accountLookups);
        apiAuthenticator.getAuthorizationProviders().addAll(authorizationProviders);
        apiAuthenticator.getIdentityProviders().addAll(identityProviders);
        apiAuthenticator.setExternalAuthProvider(externalServiceAuthProvider);

        for (TokenUtil tokenUtil : tokenUtils) {
            if (tokenUtil instanceof AbstractTokenUtil) {
                ((AbstractTokenUtil) tokenUtil).setProjectResourceManager(projectResourceManager);
            }
        }
    }

    private void setupChildSchemas() throws IOException {
        SchemaFactory projectSchemaFactory = SchemaFactoryBuilder.id("project")
                .parent(f.coreSchemaFactory)
                .jsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/project")
                .jsonAuthOverlay(f.jsonMapper,
                        "schema/auth/user-auth.json",
                        "schema/auth/project-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("superadmin")
                .parent(f.coreSchemaFactory)
                .jsonAuthOverlay(f.jsonMapper, "schema/auth/super-admin-auth.json")
                .build(c.schemaFactories);
        SchemaFactory adminSchemaFactory = SchemaFactoryBuilder.id("admin")
                .parent(f.coreSchemaFactory)
                .jsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/admin")
                .jsonAuthOverlay(f.jsonMapper,
                        "schema/auth/user-auth.json",
                        "schema/auth/admin-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("service")
                .parent(f.coreSchemaFactory)
                .jsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/service")
                .jsonAuthOverlay(f.jsonMapper,
                        "schema/auth/user-auth.json",
                        "schema/auth/admin-auth.json",
                        "schema/auth/project-auth.json",
                        "schema/auth/service-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("token")
                .parent(f.coreSchemaFactory)
                .jsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/token")
                .jsonAuthOverlay(f.jsonMapper,
                        "schema/auth/token-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("readonly")
                .parent(projectSchemaFactory)
                .notWriteable()
                .jsonAuthOverlay(f.jsonMapper,
                        "schema/auth/read-user-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("owner")
                .parent(projectSchemaFactory)
                .jsonAuthOverlay(f.jsonMapper,
                        "schema/auth/owner-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("member")
                .parent(projectSchemaFactory)
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("restricted")
                .parent(projectSchemaFactory)
                .jsonAuthOverlay(f.jsonMapper, "schema/auth/restricted-user-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("readAdmin")
                .parent(adminSchemaFactory)
                .notWriteable()
                .jsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/read-admin")
                .jsonAuthOverlay(f.jsonMapper, "schema/auth/read-admin-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("user")
                .parent(f.coreSchemaFactory)
                .jsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/user")
                .jsonAuthOverlay(f.jsonMapper, "schema/auth/user-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("environment")
                .parent(f.coreSchemaFactory)
                .jsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/environment")
                .jsonAuthOverlay(f.jsonMapper,
                        "schema/auth/user-auth.json",
                        "schema/auth/project-auth.json",
                        "schema/auth/environment-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("projectadmin")
                .parent(f.coreSchemaFactory)
                .jsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/projectadmin")
                .jsonAuthOverlay(f.jsonMapper,
                        "schema/auth/user-auth.json",
                        "schema/auth/project-auth.json",
                        "schema/auth/projectadmin-auth.json")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("agentRegister")
                .parent(f.coreSchemaFactory)
                .notWriteable()
                .whitelistJsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/agent-register")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("agent")
                .parent(f.coreSchemaFactory)
                .notWriteable()
                .whitelistJsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/agent")
                .build(c.schemaFactories);
        SchemaFactoryBuilder.id("register")
                .parent(adminSchemaFactory)
                .jsonSchemaFromPath(f.jsonMapper, f.schemaJsonMapper, f.resourceLoader, "schema/register")
                .jsonAuthOverlay(f.jsonMapper,
                        "schema/auth/register-auth.json")
                .build(c.schemaFactories);
    }

    private void setupV1Schemas() {
        String[] schemas = new String[] {
            "admin.ser",
            "member.ser",
            "owner.ser",
            "project.ser",
            "projectadmin.ser",
            "readAdmin.ser",
            "readonly.ser",
            "register.ser",
            "restricted.ser",
            "service.ser",
            "token.ser",
            "user.ser",
            "agent.ser",
            "agentRegister.ser",
            "base.ser"};

        for (String schema : schemas) {
            FileSchemaFactory schemaFactory = new FileSchemaFactory(f.schemaJsonMapper, f.coreSchemaFactory, "schema/v1/" + schema);
            c.schemaFactories.put(schemaFactory.getId(), schemaFactory);
        }
    }
}
