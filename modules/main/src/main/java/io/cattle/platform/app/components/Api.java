package io.cattle.platform.app.components;

import com.github.dockerjava.api.model.Volume;
import io.cattle.platform.api.account.AccountDeactivateActionHandler;
import io.cattle.platform.api.account.AccountFilter;
import io.cattle.platform.api.agent.AgentFilter;
import io.cattle.platform.api.apikey.ApiKeyFilter;
import io.cattle.platform.api.auditlog.AuditLogOutputFilter;
import io.cattle.platform.api.auditlog.AuditLogsRequestHandler;
import io.cattle.platform.api.certificate.CertificateCreateValidationFilter;
import io.cattle.platform.api.certificate.LoadBalancerServiceCertificateRemoveFilter;
import io.cattle.platform.api.change.impl.ResourceChangeEventProcessor;
import io.cattle.platform.api.containerevent.ContainerEventFilter;
import io.cattle.platform.api.credential.ApiKeyCertificateDownloadLinkHandler;
import io.cattle.platform.api.credential.ApiKeyOutputFilter;
import io.cattle.platform.api.data.DataManager;
import io.cattle.platform.api.doc.DocumentationHandler;
import io.cattle.platform.api.dynamicschema.DynamicSchemaFilter;
import io.cattle.platform.api.externalevent.ExternalEventFilter;
import io.cattle.platform.api.handler.ActionRequestHandler;
import io.cattle.platform.api.handler.AddClientIpHeader;
import io.cattle.platform.api.handler.CommonExceptionsHandler;
import io.cattle.platform.api.handler.DeferredActionsHandler;
import io.cattle.platform.api.handler.EventNotificationHandler;
import io.cattle.platform.api.handler.LinkRequestHandler;
import io.cattle.platform.api.handler.ResponseObjectConverter;
import io.cattle.platform.api.host.HostEvacuateActionHandler;
import io.cattle.platform.api.host.HostStoragePoolsLinkHandler;
import io.cattle.platform.api.host.HostsOutputFilter;
import io.cattle.platform.api.host.MachineConfigLinkHandler;
import io.cattle.platform.api.host.MachineOutputFilter;
import io.cattle.platform.api.host.MachineValidationFilter;
import io.cattle.platform.api.hostapi.HostApiProxyTokenImpl;
import io.cattle.platform.api.hostapi.HostApiProxyTokenManager;
import io.cattle.platform.api.hostapi.HostApiPublicCAScriptHandler;
import io.cattle.platform.api.hostapi.HostApiPublicKeyScriptHandler;
import io.cattle.platform.api.hosttemplate.HostTemplateLinkHandler;
import io.cattle.platform.api.hosttemplate.HostTemplateManager;
import io.cattle.platform.api.hosttemplate.HostTemplateOutputFilter;
import io.cattle.platform.api.hosttemplate.HostTemplateValidationFilter;
import io.cattle.platform.api.html.ConfigBasedHtmlTemplate;
import io.cattle.platform.api.instance.ContainerConvertToServiceActionHandler;
import io.cattle.platform.api.instance.ContainerCreateValidationFilter;
import io.cattle.platform.api.instance.ContainerLogsActionHandler;
import io.cattle.platform.api.instance.ContainerProxyActionHandler;
import io.cattle.platform.api.instance.ContainerUpgradeActionHandler;
import io.cattle.platform.api.instance.DockerSocketProxyActionHandler;
import io.cattle.platform.api.instance.ExecActionHandler;
import io.cattle.platform.api.instance.InstanceAgentValidationFilter;
import io.cattle.platform.api.instance.InstanceConsoleActionHandler;
import io.cattle.platform.api.instance.InstanceImageValidationFilter;
import io.cattle.platform.api.instance.InstanceManager;
import io.cattle.platform.api.instance.InstanceOutputFilter;
import io.cattle.platform.api.instance.InstancePortsValidationFilter;
import io.cattle.platform.api.instance.InstanceStopRemoveValidationFilter;
import io.cattle.platform.api.instance.InstanceVolumeCleanupStrategyValidationFilter;
import io.cattle.platform.api.machinedriver.MachineDriverFilter;
import io.cattle.platform.api.parser.ApiRequestParser;
import io.cattle.platform.api.process.ProcessInstanceReplayHandler;
import io.cattle.platform.api.processpool.ProcessPoolManager;
import io.cattle.platform.api.processsummary.ProcessSummaryManager;
import io.cattle.platform.api.pubsub.manager.PublishManager;
import io.cattle.platform.api.pubsub.manager.SubscribeManager;
import io.cattle.platform.api.pubsub.model.Publish;
import io.cattle.platform.api.pubsub.model.Subscribe;
import io.cattle.platform.api.pubsub.subscribe.jetty.JettyWebSocketSubcriptionHandler;
import io.cattle.platform.api.register.RegisterOutputFilter;
import io.cattle.platform.api.register.RegisterScriptHandler;
import io.cattle.platform.api.register.RegistrationTokenOutputFilter;
import io.cattle.platform.api.registry.RegistryServerAddressFilter;
import io.cattle.platform.api.requesthandler.BootstrapScriptsHandler;
import io.cattle.platform.api.requesthandler.ConfigurableRequestOptionsParser;
import io.cattle.platform.api.requesthandler.GenericWhitelistedProxy;
import io.cattle.platform.api.requesthandler.IdFormatterRequestHandler;
import io.cattle.platform.api.requesthandler.PostChildLinkHandler;
import io.cattle.platform.api.requesthandler.Scripts;
import io.cattle.platform.api.requesthandler.SecretsApiRequestHandler;
import io.cattle.platform.api.resource.DefaultActionHandler;
import io.cattle.platform.api.resource.DefaultResourceManager;
import io.cattle.platform.api.resource.ResourceIdInputFilter;
import io.cattle.platform.api.resource.ResourceIdOutputFilter;
import io.cattle.platform.api.secret.SecretManager;
import io.cattle.platform.api.secret.SecretValidationFilter;
import io.cattle.platform.api.service.CancelUpgradeActionHandler;
import io.cattle.platform.api.service.SelectorServiceCreateValidationFilter;
import io.cattle.platform.api.service.ServiceCertificateActionHandler;
import io.cattle.platform.api.service.ServiceCreateValidationFilter;
import io.cattle.platform.api.service.ServiceGarbageCollectActionHandler;
import io.cattle.platform.api.service.ServiceRestartValidationFilter;
import io.cattle.platform.api.service.ServiceRollbackValidationFilter;
import io.cattle.platform.api.service.ServiceStackNetworkDriverFilter;
import io.cattle.platform.api.service.ServiceStackStorageDriverFilter;
import io.cattle.platform.api.service.ServiceUpgradeValidationFilter;
import io.cattle.platform.api.service.VolumeTemplateCreateValidationFilter;
import io.cattle.platform.api.serviceevent.ServiceEventFilter;
import io.cattle.platform.api.serviceproxy.ServiceProxyManager;
import io.cattle.platform.api.setting.SettingManager;
import io.cattle.platform.api.setting.SettingsOutputFilter;
import io.cattle.platform.api.stack.AddOutputsActionHandler;
import io.cattle.platform.api.stack.ServiceDiscoveryStackOutputFilter;
import io.cattle.platform.api.stack.StackActivateServicesActionHandler;
import io.cattle.platform.api.stack.StackDeactivateServicesActionHandler;
import io.cattle.platform.api.stack.StackOutputFilter;
import io.cattle.platform.api.stats.ContainerStatsLinkHandler;
import io.cattle.platform.api.stats.HostStatsLinkHandler;
import io.cattle.platform.api.stats.ServiceContainerStatsLinkHandler;
import io.cattle.platform.api.stats.StatsOutputFilter;
import io.cattle.platform.api.storagepool.StoragePoolOutputFilter;
import io.cattle.platform.api.userpreference.UserPreferenceFilter;
import io.cattle.platform.api.util.AuthorizationResourceManagerWrapper;
import io.cattle.platform.api.volume.VolumeCreateValidationFilter;
import io.cattle.platform.api.volume.VolumeManager;
import io.cattle.platform.api.volume.VolumeOutputFilter;
import io.cattle.platform.compose.api.StackComposeLinkHandler;
import io.cattle.platform.compose.api.StackExportConfigActionHandler;
import io.cattle.platform.core.addon.ActiveSetting;
import io.cattle.platform.core.addon.ContainerEvent;
import io.cattle.platform.core.addon.ProcessPool;
import io.cattle.platform.core.addon.ProcessSummary;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.constants.RegisterConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.AuditLog;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Data;
import io.cattle.platform.core.model.ExternalEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ProcessInstance;
import io.cattle.platform.core.model.Secret;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.UserPreference;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.docker.api.model.ServiceProxy;
import io.cattle.platform.framework.encryption.request.handler.TransformationHandler;
import io.cattle.platform.iaas.api.auth.dao.impl.CredentialUniqueFilter;
import io.cattle.platform.iaas.api.auth.identity.AccountOutputFilter;
import io.cattle.platform.iaas.api.auth.impl.ApiAuthenticator;
import io.cattle.platform.iaas.api.auth.integration.local.ChangeSecretActionHandler;
import io.github.ibuildthecloud.gdapi.doc.TypeDocumentation;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.json.JacksonMapper;
import io.github.ibuildthecloud.gdapi.request.handler.BodyParserRequestHandler;
import io.github.ibuildthecloud.gdapi.request.handler.CSRFCookieHandler;
import io.github.ibuildthecloud.gdapi.request.handler.ExceptionHandler;
import io.github.ibuildthecloud.gdapi.request.handler.NotFoundHandler;
import io.github.ibuildthecloud.gdapi.request.handler.ParseCollectionAttributes;
import io.github.ibuildthecloud.gdapi.request.handler.ResourceManagerRequestHandler;
import io.github.ibuildthecloud.gdapi.request.handler.SchemasHandler;
import io.github.ibuildthecloud.gdapi.request.handler.VersionHandler;
import io.github.ibuildthecloud.gdapi.request.handler.VersionsHandler;
import io.github.ibuildthecloud.gdapi.request.handler.write.ReadWriteApiHandler;
import io.github.ibuildthecloud.gdapi.response.HtmlResponseWriter;
import io.github.ibuildthecloud.gdapi.response.JsonResponseWriter;
import io.github.ibuildthecloud.gdapi.servlet.ApiRequestFilterDelegate;
import io.github.ibuildthecloud.gdapi.validation.ReferenceValidator;
import io.github.ibuildthecloud.gdapi.validation.ResourceManagerReferenceValidator;
import io.github.ibuildthecloud.gdapi.validation.ValidationHandler;
import io.github.ibuildthecloud.gdapi.version.Versions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

public class Api {

    Common c;
    Framework f;
    DataAccess d;

    ApiRequestFilterDelegate apiRequestFilterDelegate;
    ContainerProxyActionHandler containerProxyActionHandler;
    ReferenceValidator referenceValidator;
    Versions versions;

    public Api(Framework framework, Common common, DataAccess dataAccess) throws IOException {
        this.f = framework;
        this.c = common;
        this.d = dataAccess;

        setupApiCommon();
        addHandlers();
        addResourceManagers();
        addResourceManagerFilters();
        addResourceOutputFilters();
        addLinkHandlers();
        addActionHandlers();
        setupPubSub();
        setupAuth();
        setupServlet();
    }

    private void addActionHandlers() {
        c.router.action("account.deactivate", new AccountDeactivateActionHandler(f.processManager, f.objectManager, d.accountDao));
        c.router.action("stack.addoutputs", new AddOutputsActionHandler(f.objectManager));
        c.router.action("service.cancelupgrade", new CancelUpgradeActionHandler(f.processManager, f.objectManager));
        c.router.action("credential.changesecret", new ChangeSecretActionHandler(d.passwordDao, f.jsonMapper));
        c.router.action("instance.converttoservice", new ContainerConvertToServiceActionHandler(f.objectManager, f.jsonMapper, c.revisionManager));
        c.router.action("instance.logs", new ContainerLogsActionHandler(c.hostApiService, f.objectManager));
        c.router.action("instance.proxy", new ContainerProxyActionHandler(c.hostApiService, f.objectManager));
        c.router.action("instance.upgrade", new ContainerUpgradeActionHandler(f.objectManager, f.processManager, c.revisionManager));
        c.router.action("host.dockersocket", new DockerSocketProxyActionHandler(c.hostApiService, f.objectManager));
        c.router.action("instance.execute", new ExecActionHandler(c.hostApiService, f.objectManager));
        c.router.action("host.evacuate", new HostEvacuateActionHandler(d.resourceDao));
        c.router.action("instance.console", new InstanceConsoleActionHandler(c.hostApiService, f.objectManager));
        c.router.action("processinstance.replay", new ProcessInstanceReplayHandler(f.objectManager, f.eventService));
        c.router.action("service.certificate", new ServiceCertificateActionHandler(c.certService));
        c.router.action("service.garbagecollect", new ServiceGarbageCollectActionHandler(d.serviceDao, f.processManager));
        c.router.action("stack.activateservices", new StackActivateServicesActionHandler(f.processManager, f.objectManager));
        c.router.action("stack.deactivateservices", new StackDeactivateServicesActionHandler(f.processManager, f.objectManager));
        c.router.action("stack.exportconfig", new StackExportConfigActionHandler(f.objectManager, c.composeExportService));
    }

    private void addLinkHandlers() {
        HostStatsLinkHandler hostStatsLinkHandler = new HostStatsLinkHandler(c.hostApiService, f.objectManager, c.tokenService);
        ServiceContainerStatsLinkHandler serviceContainerStatsLinkHandler = new ServiceContainerStatsLinkHandler(c.hostApiService,
                f.objectManager, c.tokenService, d.stackDao);

        c.router.link(Credential.class, new ApiKeyCertificateDownloadLinkHandler(c.keyProvider, f.objectManager));
        c.router.link(Host.class, hostStatsLinkHandler);
        c.router.link(Host.class, new MachineConfigLinkHandler(f.objectManager, c.secretsService));
        c.router.link(Host.class, new HostStoragePoolsLinkHandler(f.objectManager, d.storagePoolDao));
        c.router.link(HostTemplate.class, new HostTemplateLinkHandler(c.secretsService));
        c.router.link(Instance.class, new ContainerStatsLinkHandler(c.hostApiService, f.objectManager));
        c.router.link(ProjectConstants.TYPE, hostStatsLinkHandler);
        c.router.link(ServiceConstants.KIND_LOAD_BALANCER_SERVICE, serviceContainerStatsLinkHandler);
        c.router.link(ServiceConstants.KIND_SCALING_GROUP_SERVICE, serviceContainerStatsLinkHandler);
        c.router.link(ServiceConstants.KIND_SERVICE, serviceContainerStatsLinkHandler);
        c.router.link(Stack.class, new StackComposeLinkHandler(c.composeExportService, f.objectManager));
        c.router.link(Stack.class, serviceContainerStatsLinkHandler);
    }

    private void addResourceOutputFilters() {
        ResourceIdOutputFilter resourceIdOutputFilter = new ResourceIdOutputFilter();
        StatsOutputFilter statsOutputFilter = new StatsOutputFilter();

        c.router.outputFilter(Account.class, new AccountOutputFilter());
        c.router.outputFilter(ActiveSetting.class, new SettingsOutputFilter());
        c.router.outputFilter(AuditLog.class, new AuditLogOutputFilter());
        c.router.outputFilter(AuditLog.class, resourceIdOutputFilter);
        c.router.outputFilter(Credential.class, new ApiKeyOutputFilter());
        c.router.outputFilter(CredentialConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN, new RegistrationTokenOutputFilter());
        c.router.outputFilter(Host.class, new HostsOutputFilter(d.hostDao));
        c.router.outputFilter(Host.class, new MachineOutputFilter(c.secretsService));
        c.router.outputFilter(HostConstants.TYPE, statsOutputFilter);
        c.router.outputFilter(HostTemplate.class, new HostTemplateOutputFilter());
        c.router.outputFilter(Instance.class, new InstanceOutputFilter(d.volumeDao, c.dockerTransformer));
        c.router.outputFilter(Instance.class, statsOutputFilter);
        c.router.outputFilter(ProcessInstance.class, resourceIdOutputFilter);
        c.router.outputFilter(ProjectConstants.TYPE, statsOutputFilter);
        c.router.outputFilter(RegisterConstants.KIND_REGISTER, new RegisterOutputFilter());
        c.router.outputFilter(ServiceConstants.KIND_LOAD_BALANCER_SERVICE, statsOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_SCALING_GROUP_SERVICE, statsOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_SERVICE, statsOutputFilter);
        c.router.outputFilter(ServiceConstants.TYPE_STACK, statsOutputFilter);
        c.router.outputFilter(Setting.class, new SettingsOutputFilter());
        c.router.outputFilter(Stack.class, new ServiceDiscoveryStackOutputFilter());
        c.router.outputFilter(Stack.class, new StackOutputFilter(d.stackDao, f.objectManager));
        c.router.outputFilter(StoragePool.class, new StoragePoolOutputFilter(d.storagePoolDao));
        c.router.outputFilter(Volume.class, new VolumeOutputFilter(f.objectManager, d.volumeDao));
    }

    private void addResourceManagerFilters() {
        ResourceIdInputFilter resourceIdInputFilter = new ResourceIdInputFilter(f.idFormatter);
        CertificateCreateValidationFilter certificateCreateValidationFilter = new CertificateCreateValidationFilter();
        SelectorServiceCreateValidationFilter selectorServiceCreateValidationFilter = new SelectorServiceCreateValidationFilter(f.objectManager);
        ServiceCreateValidationFilter serviceCreateValidationFilter = new ServiceCreateValidationFilter(f.objectManager, f.processManager, c.storageService, f.jsonMapper, c.revisionManager);
        ServiceRestartValidationFilter serviceRestartValidationFilter = new ServiceRestartValidationFilter(f.objectManager);
        ServiceRollbackValidationFilter serviceRollbackValidationFilter = new ServiceRollbackValidationFilter(f.objectManager, c.revisionManager);
        ServiceStackNetworkDriverFilter serviceStackNetworkDriverFilter = new ServiceStackNetworkDriverFilter(d.networkDao, f.objectManager);
        ServiceStackStorageDriverFilter serviceStackStorageDriverFilter = new ServiceStackStorageDriverFilter(d.storagePoolDao, f.objectManager);
        ServiceUpgradeValidationFilter serviceUpgradeValidationFilter = new ServiceUpgradeValidationFilter(f.objectManager, f.jsonMapper, c.revisionManager);
        UserPreferenceFilter userPreferenceFilter = new UserPreferenceFilter(d.userPreferenceDao);

        c.router.filter(Account.class, new AccountFilter(d.accountDao));
        c.router.filter(Agent.class, new AgentFilter(c.locator, d.agentDao));
        c.router.filter(AuditLog.class, resourceIdInputFilter);
        c.router.filter(Certificate.class, certificateCreateValidationFilter);
        c.router.filter(Certificate.class, new LoadBalancerServiceCertificateRemoveFilter(f.objectManager, d.serviceDao));
        c.router.filter(ContainerEvent.class, new ContainerEventFilter(d.agentDao, f.objectManager, f.jsonMapper, f.eventService));
        c.router.filter(Credential.class, new CredentialUniqueFilter(f.coreSchemaFactory, d.passwordDao, f.jsonMapper));
        c.router.filter(CredentialConstants.KIND_API_KEY, new ApiKeyFilter());
        c.router.filter(DynamicSchemaFilter.class, new DynamicSchemaFilter(f.schemaJsonMapper, f.lockManager));
        c.router.filter(ExternalEvent.class, new ExternalEventFilter(f.objectManager));
        c.router.filter(Host.class, new MachineDriverFilter(f.objectManager));
        c.router.filter(Host.class, new MachineValidationFilter(c.secretsService));
        c.router.filter(HostTemplate.class, new HostTemplateValidationFilter());
        c.router.filter(Instance.class, new ContainerCreateValidationFilter(f.objectManager));
        c.router.filter(Instance.class, new InstanceAgentValidationFilter(f.objectManager));
        c.router.filter(Instance.class, new InstanceImageValidationFilter(c.storageService));
        c.router.filter(Instance.class, new InstanceStopRemoveValidationFilter(f.objectManager));
        c.router.filter(Instance.class, new InstanceVolumeCleanupStrategyValidationFilter());
        c.router.filter(Instance.class, new InstancePortsValidationFilter());
        c.router.filter(ProcessInstance.class, resourceIdInputFilter);
        c.router.filter(Secret.class, new SecretValidationFilter());
        c.router.filter(Service.class, serviceCreateValidationFilter);
        c.router.filter(Service.class, serviceStackNetworkDriverFilter);
        c.router.filter(Service.class, serviceStackStorageDriverFilter);
        c.router.filter(Service.class, serviceUpgradeValidationFilter);
        c.router.filter(ServiceConstants.KIND_DNS_SERVICE, serviceRollbackValidationFilter);
        c.router.filter(ServiceConstants.KIND_EXTERNAL_SERVICE, serviceRollbackValidationFilter);
        c.router.filter(ServiceConstants.KIND_LOAD_BALANCER_SERVICE, serviceRestartValidationFilter);
        c.router.filter(ServiceConstants.KIND_LOAD_BALANCER_SERVICE, serviceRollbackValidationFilter);
        c.router.filter(ServiceConstants.KIND_SCALING_GROUP_SERVICE, serviceRestartValidationFilter);
        c.router.filter(ServiceConstants.KIND_SCALING_GROUP_SERVICE, serviceRollbackValidationFilter);
        c.router.filter(ServiceConstants.KIND_SELECTOR_SERVICE, selectorServiceCreateValidationFilter);
        c.router.filter(ServiceConstants.KIND_SERVICE, selectorServiceCreateValidationFilter);
        c.router.filter(ServiceConstants.KIND_SERVICE, serviceRestartValidationFilter);
        c.router.filter(ServiceConstants.KIND_SERVICE, serviceRollbackValidationFilter);
        c.router.filter(ServiceEvent.class, new ServiceEventFilter(f.objectManager, d.agentDao, d.serviceDao));
        c.router.filter(Stack.class, serviceStackNetworkDriverFilter);
        c.router.filter(Stack.class, serviceStackStorageDriverFilter);
        c.router.filter(StoragePoolConstants.KIND_REGISTRY, new RegistryServerAddressFilter(f.objectManager));
        c.router.filter(UserPreference.class, userPreferenceFilter);
        c.router.filter(Volume.class, new VolumeCreateValidationFilter(f.objectManager));
        c.router.filter(VolumeTemplate.class, new VolumeTemplateCreateValidationFilter(f.objectManager));
    }

    private void addHandlers() throws IOException {
        c.router.handlers(
                new Scripts(Arrays.asList(
                    new BootstrapScriptsHandler(c.keyProvider),
                    new HostApiPublicCAScriptHandler(c.keyProvider),
                    new HostApiPublicKeyScriptHandler(c.hostApiService),
                    new RegisterScriptHandler(c.registrationAuthTokenManager))),
                new BodyParserRequestHandler(f.schemaJsonMapper),
                new ConfigurableRequestOptionsParser(),
                noAuthenticationProxy(),
                new ApiAuthenticator(d.authDao, f.objectManager, c.transformationService, d.accountDao),
                new SecretsApiRequestHandler(c.tokenService, d.secretsDao, f.jsonMapper, c.secretsService),
                new GenericWhitelistedProxy(f.objectManager),
                new AddClientIpHeader(),
                new IdFormatterRequestHandler(),
                new CSRFCookieHandler(),
                new PostChildLinkHandler(f.metaDataManager),
                new ParseCollectionAttributes(),
                new VersionsHandler(versions),
                new VersionHandler(),
                new SchemasHandler(),
                new ValidationHandler(referenceValidator),
                new TransformationHandler(c.transformationService),
                new LinkRequestHandler(c.locator, f.metaDataManager),
                new ReadWriteApiHandler(f.transaction,
                    new ActionRequestHandler(c.locator, f.objectManager, f.processManager),
                    new ResourceManagerRequestHandler(c.locator)),
                new CommonExceptionsHandler(),
                new NotFoundHandler(),
                new EventNotificationHandler(f.eventService),
                c.responseObjectConverter,
                new ResponseObjectConverter(f.metaDataManager, f.objectManager, c.locator),
                exceptionHandler(),
                new JsonResponseWriter(f.schemaJsonMapper),
                htmlResponseWriter(),
                new DeferredActionsHandler(),
                new AuditLogsRequestHandler(c.auditService));
    }

    private void addResourceManagers() throws IOException {
        c.router.resourceManager(Data.class, new DataManager(c.support));
        c.router.resourceManager(HostApiProxyTokenImpl.class, new HostApiProxyTokenManager(c.tokenService, d.agentDao, f.objectManager));
        c.router.resourceManager(HostTemplate.class, new HostTemplateManager(c.support, c.secretsService, f.jsonMapper));
        c.router.resourceManager(ProcessPool.class, new ProcessPoolManager(f.executorService));
        c.router.resourceManager(ProcessSummary.class, new ProcessSummaryManager(d.processSummaryDao));
        c.router.resourceManager(Secret.class, new SecretManager(c.support, c.secretsService));
        c.router.resourceManager(ServiceProxy.class, new ServiceProxyManager(d.serviceDao, containerProxyActionHandler, f.objectManager));
        c.router.resourceManager(TypeDocumentation.class, new DocumentationHandler(f.jsonMapper, f.resourceLoader.getResources("schema/base/documentation.json")));
        c.router.resourceManager(Volume.class, new VolumeManager(c.support));

        InstanceManager instanceManager = new InstanceManager(c.support, c.revisionManager, referenceValidator);
        c.router.resourceManager("instance", instanceManager);
        c.router.resourceManager("container", instanceManager);
        c.router.resourceManager("virtualMachine", instanceManager);
        c.router.resourceManager(Instance.class, instanceManager);

        SettingManager settingManager = new SettingManager(c.support, f.jooqConfig);
        c.router.resourceManager(Setting.class, new AuthorizationResourceManagerWrapper(settingManager, settingManager));
        c.router.resourceManager(ActiveSetting.class, new AuthorizationResourceManagerWrapper(settingManager, settingManager));

        c.router.defaultResourceManager(new DefaultResourceManager(c.support));
        c.router.defaultActionHandler(new DefaultActionHandler(f.objectManager, f.processManager));
    }

    private void setupPubSub() {
        ResourceChangeEventProcessor resourceChangeEventProcessor = new ResourceChangeEventProcessor(c.locator, c.responseObjectConverter, f.schemaJsonMapper);
        JettyWebSocketSubcriptionHandler jettyWebSocketSubcriptionHandler = new JettyWebSocketSubcriptionHandler(f.jsonMapper,
                f.eventService,
                f.retryTimeoutService,
                f.executorService,
                Collections.singletonList(resourceChangeEventProcessor));

        c.router.resourceManager(Subscribe.class, new SubscribeManager(jettyWebSocketSubcriptionHandler));
        c.router.resourceManager(Publish.class, new PublishManager(f.eventService));
    }

    private void setupApiCommon() {
        referenceValidator = new ResourceManagerReferenceValidator(c.locator, c.responseObjectConverter);

        Versions v = new Versions();
        v.setVersions(new HashSet<>(Arrays.asList(
                "v2-beta",
                "v2",
                "v3"
                )));
        v.setLatest("v3");
        v.setRootVersion("v2");
        versions = v;

        containerProxyActionHandler = new ContainerProxyActionHandler(c.hostApiService, f.objectManager);
    }

    private void setupAuth() throws IOException {
        Auth auth = new Auth(f, c, d);
        auth.injectAuth(c.router);
    }


    private ExceptionHandler exceptionHandler() throws IOException {
        Properties props = new Properties();
        try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("api/messages.properties")) {
            props.load(is);
        }

        ExceptionHandler exceptionHandler = new ExceptionHandler();
        exceptionHandler.setStandardErrorCodes(props);
        exceptionHandler.setMessageLocation("api/messages");

        return exceptionHandler;
    }

    private HtmlResponseWriter htmlResponseWriter() throws IOException {
        JacksonMapper jacksonMapper = new JacksonMapper();
        jacksonMapper.setEscapeForwardSlashes(true);
        jacksonMapper.init();

        return new HtmlResponseWriter(jacksonMapper, new ConfigBasedHtmlTemplate());
    }

    private GenericWhitelistedProxy noAuthenticationProxy() {
        GenericWhitelistedProxy proxy = new GenericWhitelistedProxy(f.objectManager);
        proxy.setNoAuthProxy("true");
        proxy.setAllowedPaths(Arrays.asList(
                "/v1-auth/saml",
                "/v1-webhooks/endpoint"));
        return proxy;
    }

    private void setupServlet() {
        Map<String, SchemaFactory> factories = new HashMap<>();
        factories.put("v2", f.coreSchemaFactory);
        factories.put("v2-beta", f.coreSchemaFactory);
        factories.put("v3", f.coreSchemaFactory);
        apiRequestFilterDelegate = new ApiRequestFilterDelegate(versions, new ApiRequestParser(), c.router, factories, f.idFormatter);
    }

    public ApiRequestFilterDelegate getApiRequestFilterDelegate() {
        return apiRequestFilterDelegate;
    }

    public Versions getVersions() {
        return versions;
    }

}