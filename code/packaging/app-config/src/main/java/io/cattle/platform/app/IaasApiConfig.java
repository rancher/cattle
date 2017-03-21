package io.cattle.platform.app;

import io.cattle.platform.api.handler.AddClientIpHeader;
import io.cattle.platform.api.handler.DeferredActionsHandler;
import io.cattle.platform.api.handler.EventNotificationHandler;
import io.cattle.platform.bootstrap.script.BootstrapScriptsHandler;
import io.cattle.platform.core.util.SettingsUtils;
import io.cattle.platform.docker.api.transform.TransformInspect;
import io.cattle.platform.docker.machine.api.MachineLinkFilter;
import io.cattle.platform.docker.machine.api.addon.BaseMachineConfig;
import io.cattle.platform.docker.machine.api.filter.MachineValidationFilter;
import io.cattle.platform.docker.machine.launch.SecretsApiLauncher;
import io.cattle.platform.docker.machine.launch.WebsocketProxyLauncher;
import io.cattle.platform.extension.impl.EMUtils;
import io.cattle.platform.extension.impl.ExtensionManagerImpl;
import io.cattle.platform.host.api.HostApiProxyTokenImpl;
import io.cattle.platform.host.api.HostApiProxyTokenManager;
import io.cattle.platform.host.api.HostApiPublicCAScriptHandler;
import io.cattle.platform.host.api.HostApiPublicKeyScriptHandler;
import io.cattle.platform.host.stats.api.StatsAccess;
import io.cattle.platform.host.stats.api.StatsOutputFilter;
import io.cattle.platform.iaas.api.auditing.AuditLogOutPutFilter;
import io.cattle.platform.iaas.api.auditing.AuditLogsRequestHandler;
import io.cattle.platform.iaas.api.auditing.ResourceIdInputFilter;
import io.cattle.platform.iaas.api.auditing.ResourceIdOutputFilter;
import io.cattle.platform.iaas.api.auth.AchaiusPolicyOptionsFactory;
import io.cattle.platform.iaas.api.auth.dao.impl.AuthDaoImpl;
import io.cattle.platform.iaas.api.auth.dao.impl.AuthTokenDaoImpl;
import io.cattle.platform.iaas.api.auth.dynamic.DynamicSchemaAuthorizationProvider;
import io.cattle.platform.iaas.api.auth.identity.AccountOutputFilter;
import io.cattle.platform.iaas.api.auth.identity.IdentityManager;
import io.cattle.platform.iaas.api.auth.identity.TokenResourceManager;
import io.cattle.platform.iaas.api.auth.impl.AgentQualifierAuthorizationProvider;
import io.cattle.platform.iaas.api.auth.impl.ApiAuthenticator;
import io.cattle.platform.iaas.api.auth.impl.DefaultAuthorizationProvider;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureConfigManager;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureIdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureRESTClient;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureTokenCreator;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureTokenUtil;
import io.cattle.platform.iaas.api.auth.integration.external.ExternalServiceAuthProvider;
import io.cattle.platform.iaas.api.auth.integration.external.ExternalServiceTokenUtil;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.AdminAuthLookUp;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.BasicAuthImpl;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.RancherIdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.TokenAccountLookup;
import io.cattle.platform.iaas.api.auth.integration.internal.rancher.TokenAuthLookup;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPConfigManager;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPConstantsConfig;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPIdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPTokenCreator;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPUtils;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADConfigManager;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADConstantsConfig;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADIdentityProvider;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADTokenCreator;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADTokenUtils;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthConfigManager;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthTokenCreator;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthTokenUtils;
import io.cattle.platform.iaas.api.auth.projects.ProjectMemberResourceManager;
import io.cattle.platform.iaas.api.auth.projects.ProjectResourceManager;
import io.cattle.platform.iaas.api.auth.projects.SetProjectMembersActionHandler;
import io.cattle.platform.iaas.api.change.impl.ResourceChangeEventListenerImpl;
import io.cattle.platform.iaas.api.change.impl.ResourceChangeEventProcessor;
import io.cattle.platform.iaas.api.credential.ApiKeyOutputFilter;
import io.cattle.platform.iaas.api.credential.SshKeyOutputFilter;
import io.cattle.platform.iaas.api.filter.account.AccountFilter;
import io.cattle.platform.iaas.api.filter.agent.AgentFilter;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.iaas.api.filter.compat.CompatibilityOutputFilter;
import io.cattle.platform.iaas.api.filter.containerevent.ContainerEventFilter;
import io.cattle.platform.iaas.api.filter.dynamic.schema.DynamicSchemaFilter;
import io.cattle.platform.iaas.api.filter.externalevent.ExternalEventFilter;
import io.cattle.platform.iaas.api.filter.hosts.HostsFilter;
import io.cattle.platform.iaas.api.filter.instance.InstanceAgentValidationFilter;
import io.cattle.platform.iaas.api.filter.instance.InstanceImageValidationFilter;
import io.cattle.platform.iaas.api.filter.instance.InstanceOutputFilter;
import io.cattle.platform.iaas.api.filter.instance.InstancePortsValidationFilter;
import io.cattle.platform.iaas.api.filter.instance.InstanceVolumeCleanupStrategyValidationFilter;
import io.cattle.platform.iaas.api.filter.machinedriver.MachineDriverFilter;
import io.cattle.platform.iaas.api.filter.registry.RegistryServerAddressFilter;
import io.cattle.platform.iaas.api.filter.secret.SecretValidationFilter;
import io.cattle.platform.iaas.api.filter.service.ServiceMappingsOutputFilter;
import io.cattle.platform.iaas.api.filter.serviceevent.ServiceEventFilter;
import io.cattle.platform.iaas.api.filter.snapshot.SnapshotValidationFilter;
import io.cattle.platform.iaas.api.filter.ssl.CertificateCreateValidationFilter;
import io.cattle.platform.iaas.api.filter.stack.StackOutputFilter;
import io.cattle.platform.iaas.api.filter.storagepool.StoragePoolOutputFilter;
import io.cattle.platform.iaas.api.filter.volume.VolumeOutputFilter;
import io.cattle.platform.iaas.api.manager.DataManager;
import io.cattle.platform.iaas.api.manager.HaConfigManager;
import io.cattle.platform.iaas.api.manager.InstanceManager;
import io.cattle.platform.iaas.api.manager.ProcessPoolManager;
import io.cattle.platform.iaas.api.manager.ProcessSummaryManager;
import io.cattle.platform.iaas.api.manager.SecretManager;
import io.cattle.platform.iaas.api.manager.ServiceManager;
import io.cattle.platform.iaas.api.manager.VolumeManager;
import io.cattle.platform.iaas.api.object.postinit.AccountFieldPostInitHandler;
import io.cattle.platform.iaas.api.process.ProcessInstanceReplayHandler;
import io.cattle.platform.iaas.api.request.handler.ConfigurableRequestOptionsParser;
import io.cattle.platform.iaas.api.request.handler.GenericWhitelistedProxy;
import io.cattle.platform.iaas.api.request.handler.IdFormatterRequestHandler;
import io.cattle.platform.iaas.api.request.handler.PostChildLinkHandler;
import io.cattle.platform.iaas.api.request.handler.RequestReRouterHandler;
import io.cattle.platform.iaas.api.request.handler.Scripts;
import io.cattle.platform.iaas.api.request.handler.SecretsApiRequestHandler;
import io.cattle.platform.iaas.api.user.preference.UserPreferenceDaoImpl;
import io.cattle.platform.iaas.api.user.preference.UserPreferenceFilter;
import io.cattle.platform.iaas.api.volume.VolumeCreateValidationFilter;
import io.cattle.platform.iaas.api.volume.VolumeRevertRestoreActionOutputFilter;
import io.cattle.platform.iaas.api.volume.VolumeRevertRestoreValidationFilter;
import io.cattle.platform.object.meta.TypeSet;
import io.cattle.platform.register.api.RegisterOutputFilter;
import io.cattle.platform.register.api.RegisterScriptHandler;
import io.cattle.platform.register.api.RegistrationTokenAccountLookup;
import io.cattle.platform.register.api.RegistrationTokenOutputFilter;
import io.cattle.platform.register.auth.impl.RegistrationAuthTokenManagerImpl;
import io.cattle.platform.register.dao.impl.RegistrationTokenAuthDaoImpl;
import io.cattle.platform.schema.doc.DocumentationLoader;
import io.cattle.platform.servicediscovery.api.filter.ServiceStackNetworkDriverFilter;
import io.cattle.platform.servicediscovery.api.filter.ServiceStackStorageDriverFilter;
import io.cattle.platform.spring.resource.SpringUrlListFactory;
import io.cattle.platform.systemstack.api.AccountCreateFilter;
import io.github.ibuildthecloud.gdapi.doc.FieldDocumentation;
import io.github.ibuildthecloud.gdapi.doc.TypeDocumentation;
import io.github.ibuildthecloud.gdapi.doc.handler.DocumentationHandler;
import io.github.ibuildthecloud.gdapi.id.IdentityFormatter;
import io.github.ibuildthecloud.gdapi.json.JacksonMapper;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.handler.BodyParserRequestHandler;
import io.github.ibuildthecloud.gdapi.request.handler.CSRFCookieHandler;
import io.github.ibuildthecloud.gdapi.request.handler.ExceptionHandler;
import io.github.ibuildthecloud.gdapi.request.handler.NotFoundHandler;
import io.github.ibuildthecloud.gdapi.request.handler.ParseCollectionAttributes;
import io.github.ibuildthecloud.gdapi.request.handler.ResourceManagerRequestHandler;
import io.github.ibuildthecloud.gdapi.request.handler.SchemasHandler;
import io.github.ibuildthecloud.gdapi.request.handler.VersionHandler;
import io.github.ibuildthecloud.gdapi.request.handler.VersionsHandler;
import io.github.ibuildthecloud.gdapi.request.handler.write.DefaultReadWriteApiDelegate;
import io.github.ibuildthecloud.gdapi.request.handler.write.ReadWriteApiDelegate;
import io.github.ibuildthecloud.gdapi.request.handler.write.ReadWriteApiHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.response.HtmlResponseWriter;
import io.github.ibuildthecloud.gdapi.response.JsonResponseWriter;
import io.github.ibuildthecloud.gdapi.response.ResponseObjectConverter;
import io.github.ibuildthecloud.gdapi.validation.ResourceManagerReferenceValidator;
import io.github.ibuildthecloud.gdapi.validation.ValidationHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IaasApiConfig {

    @Bean
    TransformInspect TransformInspect() {
        return new TransformInspect();
    }

    @Bean
    TypeSet MachineAddonTypeSet() {
        TypeSet typeSet = new TypeSet("MachineAddonTypeSet");
        typeSet.setTypeClasses(Arrays.<Class<?>>asList(
                BaseMachineConfig.class
                ));
        return typeSet;
    }

    @Bean
    MachineValidationFilter MachineValidationFilter() {
        return new MachineValidationFilter();
    }

    @Bean
    MachineLinkFilter MachineLinkFilter() {
        return new MachineLinkFilter();
    }

    @Bean
    AuditLogsRequestHandler AuditLogsRequestHandler() {
        return new AuditLogsRequestHandler();
    }

    @Bean
    ResourceIdOutputFilter ResourceIdOutputFilter() {
        return new ResourceIdOutputFilter();
    }

    @Bean
    AuditLogOutPutFilter AuditLogOutPutFilter() {
        return new AuditLogOutPutFilter();
    }

    @Bean
    ResourceIdInputFilter ResourceIdInputFilter() {
        return new ResourceIdInputFilter();
    }

    @Bean
    HostApiPublicKeyScriptHandler HostApiPublicKeyScriptHandler() {
        return new HostApiPublicKeyScriptHandler();
    }

    @Bean
    HostApiPublicCAScriptHandler HostApiPublicCAScriptHandler() {
        return new HostApiPublicCAScriptHandler();
    }

    @Bean
    HostApiProxyTokenManager HostApiProxyTokenManager() {
        return new HostApiProxyTokenManager();
    }

    @Bean
    WebsocketProxyLauncher WebsocketProxyLauncher() {
        return new WebsocketProxyLauncher();
    }

    @Bean
    SecretsApiLauncher SecretsApiLauncher() {
        return new SecretsApiLauncher();
    }

    @Bean
    TypeSet ProxyTokenTypeSet() {
        TypeSet typeSet = new TypeSet("ProxyTokenTypeSet");
        typeSet.setTypeClasses(Arrays.<Class<?>>asList(
                HostApiProxyTokenImpl.class
                ));
        return typeSet;
    }

    @Bean
    StatsOutputFilter StatsOutputFilter() {
        return new StatsOutputFilter();
    }

    @Bean
    TypeSet HostApiTypes() {
        TypeSet typeSet = new TypeSet("HostApiTypes");
        typeSet.setTypeClasses(Arrays.<Class<?>>asList(
                StatsAccess.class
                ));
        return typeSet;
    }

    @Bean
    DataManager DataManager() {
        return new DataManager();
    }

    @Bean
    ProcessSummaryManager ProcessSummaryManager() {
        return new ProcessSummaryManager();
    }

    @Bean
    HaConfigManager HaConfigManager(@Qualifier("FreemarkerConfig") freemarker.template.Configuration config) {
        HaConfigManager haConfig = new HaConfigManager();
        haConfig.setConfiguration(config);
        return haConfig;
    }

    @Bean
    ServiceManager ServiceManager() {
        return new ServiceManager();
    }

    @Bean
    SecretManager SecretManager() {
        return new SecretManager();
    }

    @Bean
    InstanceManager InstanceManager() {
        return new InstanceManager();
    }

    @Bean
    ProcessPoolManager ProcessPoolManager() {
        return new ProcessPoolManager();
    }

    @Bean
    VolumeManager VolumeManager() {
        return new VolumeManager();
    }

    @Bean
    ApiKeyFilter ApiKeyFilter() {
        return new ApiKeyFilter();
    }

    @Bean
    CompatibilityOutputFilter CompatibilityOutputFilter() {
        return new CompatibilityOutputFilter();
    }

    @Bean
    AgentFilter AgentFilter() {
        return new AgentFilter();
    }

    @Bean
    InstanceAgentValidationFilter InstanceAgentValidationFilter() {
        return new InstanceAgentValidationFilter();
    }

    @Bean
    InstanceImageValidationFilter InstanceImageValidationFilter() {
        return new InstanceImageValidationFilter();
    }

    @Bean
    InstancePortsValidationFilter InstancePortsValidationFilter() {
        return new InstancePortsValidationFilter();
    }

    @Bean
    InstanceVolumeCleanupStrategyValidationFilter InstanceVolumeCleanupStrategyValidationFilter() {
        return new InstanceVolumeCleanupStrategyValidationFilter();
    }

    @Bean
    SshKeyOutputFilter SshKeyOutputFilter() {
        return new SshKeyOutputFilter();
    }

    @Bean
    ApiKeyOutputFilter ApiKeyOutputFilter() {
        return new ApiKeyOutputFilter();
    }

    @Bean
    HostsFilter HostsFilter() {
        return new HostsFilter();
    }

    @Bean
    StoragePoolOutputFilter StoragePoolOutputFilter() {
        return new StoragePoolOutputFilter();
    }

    @Bean
    InstanceOutputFilter InstanceOutputFilter() {
        return new InstanceOutputFilter();
    }

    @Bean
    VolumeOutputFilter VolumeOutputFilter() {
        return new VolumeOutputFilter();
    }

    @Bean
    ContainerEventFilter ContainerEventFilter() {
        return new ContainerEventFilter();
    }

    @Bean
    ExternalEventFilter ExternalEventFilter() {
        return new ExternalEventFilter();
    }

    @Bean
    RegistryServerAddressFilter RegistryServerAddressFilter() {
        return new RegistryServerAddressFilter();
    }

    @Bean
    SecretValidationFilter SecretValidationFilter() {
        return new SecretValidationFilter();
    }

    @Bean
    ServiceEventFilter ServiceEventFilter() {
        return new ServiceEventFilter();
    }

    @Bean
    SnapshotValidationFilter SnapshotValidationFilter() {
        return new SnapshotValidationFilter();
    }

    @Bean
    CertificateCreateValidationFilter CertificateCreateValidationFilter() {
        return new CertificateCreateValidationFilter();
    }

    @Bean
    StackOutputFilter StackOutputFilter() {
        return new StackOutputFilter();
    }

    @Bean
    ProcessInstanceReplayHandler ProcessInstanceReplayHandler() {
        return new ProcessInstanceReplayHandler();
    }

    @Bean
    UserPreferenceFilter UserPreferenceFilter() {
        return new UserPreferenceFilter();
    }

    @Bean
    UserPreferenceDaoImpl UserPreferenceDaoImpl() {
        return new UserPreferenceDaoImpl();
    }

    @Bean
    DynamicSchemaFilter DynamicSchemaFilter() {
        return new DynamicSchemaFilter();
    }

    @Bean
    AccountOutputFilter AccountOutputFilter() {
        return new AccountOutputFilter();
    }

    @Bean
    AccountFilter AccountFilter() {
        return new AccountFilter();
    }

    @Bean
    AccountCreateFilter AccountCreateFilter() {
        return new AccountCreateFilter();
    }

    @Bean
    MachineDriverFilter MachineDriverFilter() {
        return new MachineDriverFilter();
    }

    @Bean
    ServiceMappingsOutputFilter ServiceMappingsOutputFilter() {
        return new ServiceMappingsOutputFilter();
    }

    @Bean
    ServiceStackStorageDriverFilter ServiceStackStorageDriverFilter() {
        return new ServiceStackStorageDriverFilter();
    }

    @Bean
    ServiceStackNetworkDriverFilter ServiceStackNetworkDriverFilter() {
        return new ServiceStackNetworkDriverFilter();
    }

    @Bean
    AccountFieldPostInitHandler AccountFieldPostInitHandler() {
        return new AccountFieldPostInitHandler();
    }

    @Bean
    Scripts Scripts(ExtensionManagerImpl em) {
        return new Scripts();
    }

    @Bean
    BodyParserRequestHandler BodyParserRequestHandler(ExtensionManagerImpl em) {
        BodyParserRequestHandler handler = new BodyParserRequestHandler();
        EMUtils.add(em, ApiRequestHandler.class, handler, "BodyParserRequestHandler");
        return handler;
    }

    @Bean
    ConfigurableRequestOptionsParser ConfigurableRequestOptionsParser(ExtensionManagerImpl em) {
        ConfigurableRequestOptionsParser handler = new ConfigurableRequestOptionsParser();
        EMUtils.add(em, ApiRequestHandler.class, handler, "ConfigurableRequestOptionsParser");
        return handler;
    }

    @Bean
    AddClientIpHeader AddClientIpHeader(ExtensionManagerImpl em) {
        AddClientIpHeader handler = new AddClientIpHeader();
        EMUtils.add(em, ApiRequestHandler.class, handler, "AddClientIpHeader");
        return handler;
    }

    @Bean
    IdFormatterRequestHandler IdFormatterRequestHandler(ExtensionManagerImpl em) {
        IdFormatterRequestHandler handler = new IdFormatterRequestHandler();
        handler.setPlainFormatter(new IdentityFormatter());
        EMUtils.add(em, ApiRequestHandler.class, handler, "IdFormatterRequestHandler");
        return handler;
    }

    @Bean
    RequestReRouterHandler RequestReRouterHandler(ExtensionManagerImpl em) {
        RequestReRouterHandler handler = new RequestReRouterHandler();
        EMUtils.add(em, ApiRequestHandler.class, handler, "RequestReRouterHandler");
        return handler;
    }

    @Bean
    CSRFCookieHandler CSRFCookieHandler(ExtensionManagerImpl em) {
        CSRFCookieHandler handler = new CSRFCookieHandler();
        EMUtils.add(em, ApiRequestHandler.class, handler, "CSRFCookieHandler");
        return handler;
    }

    @Bean
    PostChildLinkHandler PostChildLinkHandler(ExtensionManagerImpl em) {
        PostChildLinkHandler handler = new PostChildLinkHandler();
        EMUtils.add(em, ApiRequestHandler.class, handler, "PostChildLinkHandler");
        return handler;
    }

    @Bean
    ParseCollectionAttributes ParseCollectionAttributes(ExtensionManagerImpl em) {
        return EMUtils.add(em, ApiRequestHandler.class, new ParseCollectionAttributes());
    }

    @Bean
    VersionsHandler VersionsHandler(ExtensionManagerImpl em) {
        return EMUtils.add(em, ApiRequestHandler.class, new VersionsHandler());
    }

    @Bean
    SchemasHandler SchemasHandler(ExtensionManagerImpl em) {
        return EMUtils.add(em, ApiRequestHandler.class, new SchemasHandler());
    }

    @Bean
    ResourceManagerReferenceValidator ResourceManagerReferenceValidator() {
        return new ResourceManagerReferenceValidator();
    }

    @Bean
    VersionHandler VersionHandler(ExtensionManagerImpl em) {
        return EMUtils.add(em, ApiRequestHandler.class, new VersionHandler());
    }

    @Bean
    ValidationHandler ValidationHandler(ExtensionManagerImpl em, ResourceManagerReferenceValidator validator) {
        ValidationHandler handler = EMUtils.add(em, ApiRequestHandler.class, new ValidationHandler());
        handler.setReferenceValidator(validator);
        return handler;
    }

    @Bean
    ReadWriteApiDelegate DefaultReadWriteApiDelegate() {
        return new DefaultReadWriteApiDelegate();
    }

    @Bean
    ReadWriteApiHandler ResourceManagerRequestHandler(ExtensionManagerImpl em, ReadWriteApiDelegate delegate, ResourceManagerLocator locator) {
        ResourceManagerRequestHandler inner = new ResourceManagerRequestHandler();
        inner.setResourceManagerLocator(locator);
        ReadWriteApiHandler handler = EMUtils.add(em, ApiRequestHandler.class, new ReadWriteApiHandler());
        delegate.setHandlers(Arrays.<ApiRequestHandler>asList(
                inner
                ));
        handler.setDelegate(delegate);
        return handler;
    }

    @Bean
    ResourceManagerRequestHandler InnerResourceManagerRequestHandler() {
        return new ResourceManagerRequestHandler();
    }

    @Bean
    NotFoundHandler NotFoundHandler(ExtensionManagerImpl em) {
        return EMUtils.add(em, ApiRequestHandler.class, new NotFoundHandler());
    }

    @Bean
    EventNotificationHandler EventNotificationHandler(ExtensionManagerImpl em) {
        return EMUtils.add(em, ApiRequestHandler.class, new EventNotificationHandler());
    }

    @Bean
    ResponseObjectConverter ResponseObjectConverter(ExtensionManagerImpl em) {
        return EMUtils.add(em, ApiRequestHandler.class, new ResponseObjectConverter());
    }

    @Bean
    ExceptionHandler ExceptionHandler(ExtensionManagerImpl em) throws IOException {
        Properties props = new Properties();
        try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("api/messages.properties")) {
            props.load(is);
        }

        ExceptionHandler handler = EMUtils.add(em, ApiRequestHandler.class, new ExceptionHandler());
        handler.setStandardErrorCodes(props);
        handler.setMessageLocation("api/messages");
        return handler;
    }

    @Bean
    JsonResponseWriter JsonResponseWriter(ExtensionManagerImpl em, JsonMapper jsonMapper) {
        JsonResponseWriter writer = EMUtils.add(em, ApiRequestHandler.class, new JsonResponseWriter());
        writer.setJsonMapper(jsonMapper);
        return writer;
    }

    @Bean
    HtmlResponseWriter HtmlResponseWriter(ExtensionManagerImpl em) {
        HtmlResponseWriter writer = EMUtils.add(em, ApiRequestHandler.class, new HtmlResponseWriter());
        JacksonMapper jacksonMapper = new JacksonMapper();
        jacksonMapper.setEscapeForwardSlashes(true);
        jacksonMapper.init();
        writer.setJsonMapper(jacksonMapper);
        return writer;
    }

    @Bean
    DeferredActionsHandler DeferredActionsHandler(ExtensionManagerImpl em) {
        return EMUtils.add(em, ApiRequestHandler.class, new DeferredActionsHandler());
    }

    @Bean
    DynamicSchemaAuthorizationProvider DefaultAuthorizationProvider(@Qualifier("defaultAuthorizationProvider") DefaultAuthorizationProvider defaultProvider) {
        DynamicSchemaAuthorizationProvider provider = new DynamicSchemaAuthorizationProvider();
        provider.setAuthorizationProvider(defaultProvider);
        return provider;
    }

    @Bean
    DefaultAuthorizationProvider defaultAuthorizationProvider() {
        return new DefaultAuthorizationProvider();
    }

    @Bean
    AgentQualifierAuthorizationProvider AgentQualifierAuthorizationProvider() {
        return new AgentQualifierAuthorizationProvider();
    }

    @Bean
    AchaiusPolicyOptionsFactory AchaiusPolicyOptionsFactory() {
        return new AchaiusPolicyOptionsFactory();
    }

    @Bean
    ResourceChangeEventListenerImpl ResourceChangeEventListenerImpl() {
        return new ResourceChangeEventListenerImpl();
    }

    @Bean
    ResourceChangeEventProcessor ResourceChangeEventProcessor() {
        return new ResourceChangeEventProcessor();
    }

    @Bean
    TypeSet DocTypes() {
        TypeSet typeSet = new TypeSet("DocTypes");
        typeSet.setTypeClasses(Arrays.<Class<?>>asList(
                TypeDocumentation.class,
                FieldDocumentation.class
                ));
        return typeSet;
    }

    @Bean
    DocumentationHandler DocumentationHandler() {
        return new DocumentationHandler();
    }

    @Bean
    SpringUrlListFactory DocsLocation() {
        SpringUrlListFactory factory = new SpringUrlListFactory();
        factory.setResources(Arrays.asList(
            "classpath:schema/base/documentation.json",
            "classpath*:schema/base/documentation.json.d/**/*.json"
            ));
        return factory;
    }

    @SuppressWarnings("unchecked")
    @Bean
    DocumentationLoader DocumentationLoader(@Qualifier("DocsLocation") List<?> resources) {
        DocumentationLoader loader = new DocumentationLoader();
        loader.setResources((List<URL>) resources);
        return loader;
    }

    @Bean
    TokenResourceManager TokenResourceManager() {
        return new TokenResourceManager();
    }

    @Bean
    ApiAuthenticator ApiAuthenticator() {
        return new ApiAuthenticator();
    }

    @Bean
    AuthDaoImpl AuthDaoImpl() {
        return new AuthDaoImpl();
    }

    @Bean
    BasicAuthImpl BasicAuthImpl() {
        return new BasicAuthImpl();
    }

    @Bean
    AdminAuthLookUp AdminAuthLookUp() {
        return new AdminAuthLookUp();
    }

    @Bean
    RancherIdentityProvider RancherIdentityProvider() {
        return new RancherIdentityProvider();
    }

    @Bean
    ProjectResourceManager ProjectResourceManager() {
        return new ProjectResourceManager();
    }

    @Bean
    ProjectMemberResourceManager ProjectMemberResourceManager() {
        return new ProjectMemberResourceManager();
    }

    @Bean
    SetProjectMembersActionHandler SetProjectMembersActionHandler() {
        return new SetProjectMembersActionHandler();
    }

    @Bean
    TokenAuthLookup TokenAuthLookup() {
        return new TokenAuthLookup();
    }

    @Bean
    ExternalServiceAuthProvider ExternalServiceAuthProvider() {
        return new ExternalServiceAuthProvider();
    }

    @Bean
    ExternalServiceTokenUtil ExternalServiceTokenUtil() {
        return new ExternalServiceTokenUtil();
    }

    @Bean
    TokenAccountLookup TokenAccountLookup() {
        return new TokenAccountLookup();
    }

    @Bean
    GenericWhitelistedProxy NoAuthenticationProxy() {
        GenericWhitelistedProxy proxy = new GenericWhitelistedProxy("NoAuthenticationProxy");
        proxy.setNoAuthProxy("true");
        proxy.setAllowedPaths(Arrays.asList(
                "/v1-auth/saml",
                "/v1-webhooks/endpoint"));
        return proxy;
    }

    @Bean
    SecretsApiRequestHandler SecretsApiRequestHandler() {
        return new SecretsApiRequestHandler();
    }

    @Bean
    GenericWhitelistedProxy GenericWhitelistedProxy() {
        return new GenericWhitelistedProxy("GenericWhitelistedProxy");
    }

    @Bean
    ADIdentityProvider ADIdentityProvider(@Qualifier("CoreExecutorService") ExecutorService es) {
        ADIdentityProvider provider = new ADIdentityProvider();
        provider.setExecutorService(es);
        return provider;
    }

    @Bean
    OpenLDAPIdentityProvider OpenLDAPIdentityProvider(@Qualifier("CoreExecutorService") ExecutorService es) {
        OpenLDAPIdentityProvider provider = new OpenLDAPIdentityProvider();
        provider.setExecutorService(es);
        return provider;
    }

    @Bean
    ADTokenCreator ADTokenCreator() {
        return new ADTokenCreator();
    }

    @Bean
    ADTokenUtils ADTokenUtils() {
        return new ADTokenUtils();
    }

    @Bean
    ADConfigManager ADConfigManager() {
        return new ADConfigManager();
    }

    @Bean
    ADConstantsConfig ADConstantsConfig() {
        return new ADConstantsConfig();
    }

    @Bean
    OpenLDAPTokenCreator OpenLDAPTokenCreator() {
        return new OpenLDAPTokenCreator();
    }

    @Bean
    OpenLDAPUtils OpenLDAPUtils() {
        return new OpenLDAPUtils();
    }

    @Bean
    OpenLDAPConfigManager OpenLDAPConfigManager() {
        return new OpenLDAPConfigManager();
    }

    @Bean
    OpenLDAPConstantsConfig OpenLDAPConstantsConfig() {
        return new OpenLDAPConstantsConfig();
    }

    @Bean
    SettingsUtils SettingsUtils() {
        return new SettingsUtils();
    }

    @Bean
    IdentityManager IdentityManager(@Qualifier("CoreExecutorService") ExecutorService es) {
        IdentityManager manager = new IdentityManager();
        manager.setExecutorService(es);
        return manager;
    }

    @Bean
    AuthTokenDaoImpl AuthTokenDaoImpl() {
        return new AuthTokenDaoImpl();
    }

    @Bean
    LocalAuthConfigManager LocalAuthConfigManager() {
        return new LocalAuthConfigManager();
    }

    @Bean
    LocalAuthTokenCreator LocalAuthTokenCreator() {
        return new LocalAuthTokenCreator();
    }

    @Bean
    LocalAuthTokenUtils LocalAuthTokenUtils() {
        return new LocalAuthTokenUtils();
    }

    @Bean
    AzureIdentityProvider AzureIdentityProvider() {
        return new AzureIdentityProvider();
    }

    @Bean
    AzureTokenCreator AzureTokenCreator() {
        return new AzureTokenCreator();
    }

    @Bean
    AzureTokenUtil AzureTokenUtil() {
        return new AzureTokenUtil();
    }

    @Bean
    AzureConfigManager AzureConfigManager() {
        return new AzureConfigManager();
    }

    @Bean
    AzureRESTClient AzureRESTClient() {
        return new AzureRESTClient();
    }

    @Bean
    BootstrapScriptsHandler BootstrapScriptsHandler() {
        return new BootstrapScriptsHandler();
    }

    @Bean
    RegistrationTokenOutputFilter RegistrationTokenOutputFilter() {
        return new RegistrationTokenOutputFilter();
    }

    @Bean
    RegisterOutputFilter RegisterOutputFilter() {
        return new RegisterOutputFilter();
    }

    @Bean
    RegisterScriptHandler RegisterScriptHandler() {
        return new RegisterScriptHandler();
    }

    @Bean
    RegistrationTokenAccountLookup RegistrationTokenAccountLookup() {
        return new RegistrationTokenAccountLookup();
    }

    @Bean
    RegistrationAuthTokenManagerImpl RegistrationAuthTokenManagerImpl() {
        return new RegistrationAuthTokenManagerImpl();
    }

    @Bean
    RegistrationTokenAuthDaoImpl RegistrationTokenAuthDaoImpl() {
        return new RegistrationTokenAuthDaoImpl();
    }

    @Bean
    VolumeRevertRestoreActionOutputFilter VolumeRevertRestoreActionOutputFilter() {
        return new VolumeRevertRestoreActionOutputFilter();
    }

    @Bean
    VolumeRevertRestoreValidationFilter VolumeRevertRestoreValidationFilter() {
        return new VolumeRevertRestoreValidationFilter();
    }

    @Bean
    VolumeCreateValidationFilter VolumeCreateValidationFilter() {
        return new VolumeCreateValidationFilter();
    }

}
