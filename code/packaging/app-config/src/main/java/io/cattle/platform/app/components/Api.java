package io.cattle.platform.app.components;

import io.cattle.platform.api.handler.ActionRequestHandler;
import io.cattle.platform.api.handler.AddClientIpHeader;
import io.cattle.platform.api.handler.CommonExceptionsHandler;
import io.cattle.platform.api.handler.DeferredActionsHandler;
import io.cattle.platform.api.handler.EventNotificationHandler;
import io.cattle.platform.api.handler.LinkRequestHandler;
import io.cattle.platform.api.handler.ResponseObjectConverter;
import io.cattle.platform.api.html.ConfigBasedHtmlTemplate;
import io.cattle.platform.api.parser.ApiRequestParser;
import io.cattle.platform.api.pubsub.manager.PublishManager;
import io.cattle.platform.api.pubsub.manager.SubscribeManager;
import io.cattle.platform.api.pubsub.model.Publish;
import io.cattle.platform.api.pubsub.model.Subscribe;
import io.cattle.platform.api.pubsub.subscribe.jetty.JettyWebSocketSubcriptionHandler;
import io.cattle.platform.api.resource.DefaultActionHandler;
import io.cattle.platform.api.resource.DefaultResourceManager;
import io.cattle.platform.bootstrap.script.BootstrapScriptsHandler;
import io.cattle.platform.compose.api.StackComposeLinkHandler;
import io.cattle.platform.compose.api.StackExportConfigActionHandler;
import io.cattle.platform.core.addon.ActiveSetting;
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
import io.cattle.platform.core.model.Secret;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.UserPreference;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.docker.api.ContainerLogsActionHandler;
import io.cattle.platform.docker.api.ContainerProxyActionHandler;
import io.cattle.platform.docker.api.DockerSocketProxyActionHandler;
import io.cattle.platform.docker.api.ExecActionHandler;
import io.cattle.platform.docker.api.container.ServiceProxyManager;
import io.cattle.platform.docker.api.model.ServiceProxy;
import io.cattle.platform.docker.api.transform.TransformInspect;
import io.cattle.platform.docker.machine.api.MachineConfigLinkHandler;
import io.cattle.platform.docker.machine.api.MachineLinkFilter;
import io.cattle.platform.docker.machine.api.filter.MachineOutputFilter;
import io.cattle.platform.docker.machine.api.filter.MachineValidationFilter;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.framework.encryption.request.handler.TransformationHandler;
import io.cattle.platform.host.api.HostApiProxyTokenImpl;
import io.cattle.platform.host.api.HostApiProxyTokenManager;
import io.cattle.platform.host.api.HostApiPublicCAScriptHandler;
import io.cattle.platform.host.api.HostApiPublicKeyScriptHandler;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.host.stats.api.ContainerStatsLinkHandler;
import io.cattle.platform.host.stats.api.HostStatsLinkHandler;
import io.cattle.platform.host.stats.api.ServiceContainerStatsLinkHandler;
import io.cattle.platform.host.stats.api.StatsOutputFilter;
import io.cattle.platform.iaas.api.account.AccountDeactivateActionHandler;
import io.cattle.platform.iaas.api.auditing.AuditLogOutputFilter;
import io.cattle.platform.iaas.api.auditing.AuditLogsRequestHandler;
import io.cattle.platform.iaas.api.auditing.ResourceIdInputFilter;
import io.cattle.platform.iaas.api.auditing.ResourceIdOutputFilter;
import io.cattle.platform.iaas.api.auth.dao.impl.CredentialUniqueFilter;
import io.cattle.platform.iaas.api.auth.identity.AccountOutputFilter;
import io.cattle.platform.iaas.api.auth.impl.ApiAuthenticator;
import io.cattle.platform.iaas.api.auth.integration.local.ChangeSecretActionHandler;
import io.cattle.platform.iaas.api.change.impl.ResourceChangeEventProcessor;
import io.cattle.platform.iaas.api.container.ContainerUpgradeActionHandler;
import io.cattle.platform.iaas.api.credential.ApiKeyCertificateDownloadLinkHandler;
import io.cattle.platform.iaas.api.credential.ApiKeyOutputFilter;
import io.cattle.platform.iaas.api.doc.DocumentationHandler;
import io.cattle.platform.iaas.api.filter.account.AccountFilter;
import io.cattle.platform.iaas.api.filter.agent.AgentFilter;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.iaas.api.filter.compat.CompatibilityOutputFilter;
import io.cattle.platform.iaas.api.filter.containerevent.ContainerEventFilter;
import io.cattle.platform.iaas.api.filter.dynamic.schema.DynamicSchemaFilter;
import io.cattle.platform.iaas.api.filter.externalevent.ExternalEventFilter;
import io.cattle.platform.iaas.api.filter.hosts.HostsFilter;
import io.cattle.platform.iaas.api.filter.instance.ContainerCreateValidationFilter;
import io.cattle.platform.iaas.api.filter.instance.InstanceAgentValidationFilter;
import io.cattle.platform.iaas.api.filter.instance.InstanceImageValidationFilter;
import io.cattle.platform.iaas.api.filter.instance.InstanceOutputFilter;
import io.cattle.platform.iaas.api.filter.instance.InstanceVolumeCleanupStrategyValidationFilter;
import io.cattle.platform.iaas.api.filter.machinedriver.MachineDriverFilter;
import io.cattle.platform.iaas.api.filter.registry.RegistryServerAddressFilter;
import io.cattle.platform.iaas.api.filter.secret.SecretValidationFilter;
import io.cattle.platform.iaas.api.filter.service.ServiceMappingsOutputFilter;
import io.cattle.platform.iaas.api.filter.serviceevent.ServiceEventFilter;
import io.cattle.platform.iaas.api.filter.settings.AuthorizationResourceManagerWrapper;
import io.cattle.platform.iaas.api.filter.settings.SettingManager;
import io.cattle.platform.iaas.api.filter.settings.SettingsOutputFilter;
import io.cattle.platform.iaas.api.filter.ssl.CertificateCreateValidationFilter;
import io.cattle.platform.iaas.api.filter.stack.StackOutputFilter;
import io.cattle.platform.iaas.api.filter.storagepool.StoragePoolOutputFilter;
import io.cattle.platform.iaas.api.filter.volume.VolumeOutputFilter;
import io.cattle.platform.iaas.api.host.HostEvacuateActionHandler;
import io.cattle.platform.iaas.api.host.HostTemplateLinkHandler;
import io.cattle.platform.iaas.api.host.HostTemplateOutputFilter;
import io.cattle.platform.iaas.api.host.HostTemplateValidationFilter;
import io.cattle.platform.iaas.api.manager.DataManager;
import io.cattle.platform.iaas.api.manager.HostTemplateManager;
import io.cattle.platform.iaas.api.manager.InstanceManager;
import io.cattle.platform.iaas.api.manager.ProcessPoolManager;
import io.cattle.platform.iaas.api.manager.ProcessSummaryManager;
import io.cattle.platform.iaas.api.manager.SecretManager;
import io.cattle.platform.iaas.api.manager.VolumeManager;
import io.cattle.platform.iaas.api.process.ProcessInstanceReplayHandler;
import io.cattle.platform.iaas.api.request.handler.ConfigurableRequestOptionsParser;
import io.cattle.platform.iaas.api.request.handler.GenericWhitelistedProxy;
import io.cattle.platform.iaas.api.request.handler.IdFormatterRequestHandler;
import io.cattle.platform.iaas.api.request.handler.PostChildLinkHandler;
import io.cattle.platform.iaas.api.request.handler.RequestReRouterHandler;
import io.cattle.platform.iaas.api.request.handler.Scripts;
import io.cattle.platform.iaas.api.request.handler.SecretsApiRequestHandler;
import io.cattle.platform.iaas.api.service.ServiceCertificateActionHandler;
import io.cattle.platform.iaas.api.task.TaskExecuteActionHandler;
import io.cattle.platform.iaas.api.user.preference.UserPreferenceFilter;
import io.cattle.platform.iaas.api.volume.VolumeCreateValidationFilter;
import io.cattle.platform.register.api.RegisterOutputFilter;
import io.cattle.platform.register.api.RegisterScriptHandler;
import io.cattle.platform.register.api.RegistrationTokenOutputFilter;
import io.cattle.platform.servicediscovery.api.action.AddOutputsActionHandler;
import io.cattle.platform.servicediscovery.api.action.AddServiceLinkActionHandler;
import io.cattle.platform.servicediscovery.api.action.CancelUpgradeActionHandler;
import io.cattle.platform.servicediscovery.api.action.ContainerConvertToServiceActionHandler;
import io.cattle.platform.servicediscovery.api.action.RemoveServiceLinkActionHandler;
import io.cattle.platform.servicediscovery.api.action.ServiceGarbageCollectActionHandler;
import io.cattle.platform.servicediscovery.api.action.SetServiceLinksActionHandler;
import io.cattle.platform.servicediscovery.api.action.StackActivateServicesActionHandler;
import io.cattle.platform.servicediscovery.api.action.StackDeactivateServicesActionHandler;
import io.cattle.platform.servicediscovery.api.filter.InstanceStopRemoveValidationFilter;
import io.cattle.platform.servicediscovery.api.filter.LoadBalancerServiceCertificateRemoveFilter;
import io.cattle.platform.servicediscovery.api.filter.SelectorServiceCreateValidationFilter;
import io.cattle.platform.servicediscovery.api.filter.ServiceAddRemoveLinkServiceValidationFilter;
import io.cattle.platform.servicediscovery.api.filter.ServiceCreateValidationFilter;
import io.cattle.platform.servicediscovery.api.filter.ServiceDiscoveryStackOutputFilter;
import io.cattle.platform.servicediscovery.api.filter.ServiceRestartValidationFilter;
import io.cattle.platform.servicediscovery.api.filter.ServiceRollbackValidationFilter;
import io.cattle.platform.servicediscovery.api.filter.ServiceSetServiceLinksValidationFilter;
import io.cattle.platform.servicediscovery.api.filter.ServiceStackNetworkDriverFilter;
import io.cattle.platform.servicediscovery.api.filter.ServiceStackStorageDriverFilter;
import io.cattle.platform.servicediscovery.api.filter.ServiceUpgradeValidationFilter;
import io.cattle.platform.servicediscovery.api.filter.StackCreateValidationFilter;
import io.cattle.platform.servicediscovery.api.filter.VolumeTemplateCreateValidationFilter;
import io.cattle.platform.storage.api.filter.ExternalTemplateInstanceFilter;
import io.cattle.platform.systemstack.api.AccountCreateFilter;
import io.cattle.platform.vm.api.InstanceConsoleActionHandler;
import io.github.ibuildthecloud.gdapi.doc.TypeDocumentation;
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
import io.github.ibuildthecloud.gdapi.response.ResponseConverter;
import io.github.ibuildthecloud.gdapi.servlet.ApiRequestFilterDelegate;
import io.github.ibuildthecloud.gdapi.validation.ReferenceValidator;
import io.github.ibuildthecloud.gdapi.validation.ResourceManagerReferenceValidator;
import io.github.ibuildthecloud.gdapi.validation.ValidationHandler;
import io.github.ibuildthecloud.gdapi.version.Versions;

import java.awt.event.ContainerEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Stack;

import com.github.dockerjava.api.model.Volume;

public class Api {

    Common c;
    Framework f;
    DataAccess d;

    ApiRequestFilterDelegate apiRequestFilterDelegate;
    Auth auth;
    ContainerProxyActionHandler containerProxyActionHandler;
    HostApiService hostApiService;
    ReferenceValidator referenceValidator;
    ResponseConverter responseConverter;
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
        c.router.action("service.addservicelink", new AddServiceLinkActionHandler(d.serviceConsumeMapDao));
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
        c.router.action("service.removeservicelink", new RemoveServiceLinkActionHandler(d.serviceConsumeMapDao));
        c.router.action("service.certificate", new ServiceCertificateActionHandler(c.certService));
        c.router.action("service.garbagecollect", new ServiceGarbageCollectActionHandler(d.serviceDao, f.processManager));
        c.router.action("service.setservicelinks", new SetServiceLinksActionHandler(d.serviceConsumeMapDao, f.lockManager, f.objectManager,
                f.idFormatter));
        c.router.action("stack.activateservices", new StackActivateServicesActionHandler(f.processManager, f.objectManager, d.serviceConsumeMapDao));
        c.router.action("stack.deactivateservices", new StackDeactivateServicesActionHandler(f.processManager, f.objectManager));
        c.router.action("stack.exportconfig", new StackExportConfigActionHandler(f.objectManager, c.composeExportService));
        c.router.action("task.execute", new TaskExecuteActionHandler(c.taskManager));
    }

    private void addLinkHandlers() {
        HostStatsLinkHandler hostStatsLinkHandler = new HostStatsLinkHandler(c.hostApiService, f.objectManager, c.tokenService);
        ServiceContainerStatsLinkHandler serviceContainerStatsLinkHandler = new ServiceContainerStatsLinkHandler(c.hostApiService,
                f.objectManager, c.tokenService, d.serviceExposeMapDao, d.stackDao);

        c.router.link(Credential.class, new ApiKeyCertificateDownloadLinkHandler(c.keyProvider, f.objectManager));
        c.router.link(Host.class, hostStatsLinkHandler);
        c.router.link(Host.class, new MachineConfigLinkHandler(f.objectManager, c.secretsService));
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
        CompatibilityOutputFilter compabilityOutputFilter = new CompatibilityOutputFilter();
        ResourceIdOutputFilter resourceIdOutputFilter = new ResourceIdOutputFilter();
        StatsOutputFilter statsOutputFilter = new StatsOutputFilter();

        // Before for some reason...
        c.router.outputFilter(Host.class, new HostsFilter(d.hostDao));

        c.router.outputFilter(Account.class, new AccountOutputFilter());
        c.router.outputFilter(ActiveSetting.class, new SettingsOutputFilter());
        c.router.outputFilter(Credential.class, new ApiKeyOutputFilter());
        c.router.outputFilter(AuditLog.class, new AuditLogOutputFilter());
        c.router.outputFilter(ServiceConstants.TYPE_STACK, compabilityOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_SERVICE, compabilityOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_KUBERNETES_SERVICE, compabilityOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_LOAD_BALANCER_SERVICE, compabilityOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_DNS_SERVICE, compabilityOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_EXTERNAL_SERVICE, compabilityOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_SELECTOR_SERVICE, compabilityOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_SCALING_GROUP_SERVICE, compabilityOutputFilter);
        c.router.outputFilter(HostTemplate.class, new HostTemplateOutputFilter());
        c.router.outputFilter(Host.class, new MachineLinkFilter());
        c.router.outputFilter(Host.class, new MachineOutputFilter(c.secretsService));
        c.router.outputFilter(RegisterConstants.KIND_REGISTER, new RegisterOutputFilter());
        c.router.outputFilter(CredentialConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN, new RegistrationTokenOutputFilter());
        c.router.outputFilter(AuditLog.class, resourceIdOutputFilter);
        c.router.outputFilter(ProcessInstance.class, resourceIdOutputFilter);
        c.router.outputFilter(Setting.class, new SettingsOutputFilter());
        c.router.outputFilter(Stack.class, new ServiceDiscoveryStackOutputFilter());
        c.router.outputFilter(HostConstants.TYPE, statsOutputFilter);
        c.router.outputFilter(ProjectConstants.TYPE, statsOutputFilter);
        c.router.outputFilter(ServiceConstants.TYPE_STACK, statsOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_SERVICE, statsOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_LOAD_BALANCER_SERVICE, statsOutputFilter);
        c.router.outputFilter(ServiceConstants.KIND_SCALING_GROUP_SERVICE, statsOutputFilter);

        c.router.outputFilter(Instance.class, new InstanceOutputFilter(d.serviceDao, d.volumeDao, c.dockerTransformer));
        c.router.outputFilter(Service.class, new ServiceMappingsOutputFilter(d.serviceDao, f.objectManager));
        c.router.outputFilter(Stack.class, new StackOutputFilter(d.stackDao, f.objectManager));
        c.router.outputFilter(StoragePool.class, new StoragePoolOutputFilter(d.storagePoolDao));
        c.router.outputFilter(Volume.class, new VolumeOutputFilter(f.objectManager, d.volumeDao));
    }

    private void addResourceManagerFilters() {
        ResourceIdInputFilter resourceIdInputFilter = new ResourceIdInputFilter(f.idFormatter);
        CertificateCreateValidationFilter certificateCreateValidationFilter = new CertificateCreateValidationFilter();
        SelectorServiceCreateValidationFilter selectorServiceCreateValidationFilter = new SelectorServiceCreateValidationFilter(f.objectManager);
        ServiceAddRemoveLinkServiceValidationFilter serviceAddRemoveLinkServiceValidationFilter = new ServiceAddRemoveLinkServiceValidationFilter(d.serviceConsumeMapDao, f.objectManager, f.jsonMapper);
        ServiceCreateValidationFilter serviceCreateValidationFilter = new ServiceCreateValidationFilter(f.objectManager, f.processManager, c.storageService, f.jsonMapper, c.revisionManager);
        ServiceRestartValidationFilter serviceRestartValidationFilter = new ServiceRestartValidationFilter(f.objectManager, f.jsonMapper, d.serviceExposeMapDao);
        ServiceRollbackValidationFilter serviceRollbackValidationFilter = new ServiceRollbackValidationFilter(f.objectManager, c.revisionManager);
        ServiceSetServiceLinksValidationFilter serviceSetServiceLinksValidationFilter = new ServiceSetServiceLinksValidationFilter(f.objectManager);
        ServiceStackNetworkDriverFilter serviceStackNetworkDriverFilter = new ServiceStackNetworkDriverFilter(d.networkDao, f.objectManager);
        ServiceStackStorageDriverFilter serviceStackStorageDriverFilter = new ServiceStackStorageDriverFilter(d.storagePoolDao, f.objectManager);
        ServiceUpgradeValidationFilter serviceUpgradeValidationFilter = new ServiceUpgradeValidationFilter(f.objectManager, f.jsonMapper, c.revisionManager);
        StackCreateValidationFilter stackCreateValidationFilter = new StackCreateValidationFilter(c.locator, f.objectManager);
        UserPreferenceFilter userPreferenceFilter = new UserPreferenceFilter(d.userPreferenceDao);

        c.router.filter(Account.class, new AccountCreateFilter(f.objectManager, f.jsonMapper));
        c.router.filter(Account.class, new AccountFilter(d.accountDao));
        c.router.filter(Agent.class, new AgentFilter(c.locator, d.agentDao));
        c.router.filter(AuditLog.class, resourceIdInputFilter);
        c.router.filter(Certificate.class, certificateCreateValidationFilter);
        c.router.filter(Certificate.class, new LoadBalancerServiceCertificateRemoveFilter(f.objectManager, d.serviceDao));
        c.router.filter(ContainerEvent.class, new ContainerEventFilter(d.agentDao, d.containerEventDao, f.objectManager));
        c.router.filter(Credential.class, new CredentialUniqueFilter(f.coreSchemaFactory, d.passwordDao, f.jsonMapper));
        c.router.filter(CredentialConstants.KIND_API_KEY, new ApiKeyFilter());
        c.router.filter(DynamicSchemaFilter.class, new DynamicSchemaFilter(f.schemaJsonMapper, f.lockManager));
        c.router.filter(ExternalEvent.class, new ExternalEventFilter(f.objectManager));
        c.router.filter(Host.class, new MachineDriverFilter(f.objectManager));
        c.router.filter(Host.class, new MachineValidationFilter(c.secretsService));
        c.router.filter(HostTemplate.class, new HostTemplateValidationFilter());
        c.router.filter(Instance.class, certificateCreateValidationFilter);
        c.router.filter(Instance.class, new ContainerCreateValidationFilter(f.objectManager));
        c.router.filter(Instance.class, new ExternalTemplateInstanceFilter(c.storageService));
        c.router.filter(Instance.class, new InstanceAgentValidationFilter(f.objectManager));
        c.router.filter(Instance.class, new InstanceImageValidationFilter());
        c.router.filter(Instance.class, new InstanceStopRemoveValidationFilter(f.objectManager));
        c.router.filter(Instance.class, new InstanceVolumeCleanupStrategyValidationFilter());
        c.router.filter(ProcessInstance.class, resourceIdInputFilter);
        c.router.filter(Secret.class, new SecretValidationFilter());
        c.router.filter(Service.class, serviceCreateValidationFilter);
        c.router.filter(Service.class, serviceStackNetworkDriverFilter);
        c.router.filter(Service.class, serviceStackStorageDriverFilter);
        c.router.filter(Service.class, serviceUpgradeValidationFilter);
        c.router.filter(ServiceConstants.KIND_DNS_SERVICE, serviceAddRemoveLinkServiceValidationFilter);
        c.router.filter(ServiceConstants.KIND_DNS_SERVICE, serviceRollbackValidationFilter);
        c.router.filter(ServiceConstants.KIND_DNS_SERVICE, serviceSetServiceLinksValidationFilter);
        c.router.filter(ServiceConstants.KIND_EXTERNAL_SERVICE, serviceRollbackValidationFilter);
        c.router.filter(ServiceConstants.KIND_LOAD_BALANCER_SERVICE, serviceAddRemoveLinkServiceValidationFilter);
        c.router.filter(ServiceConstants.KIND_LOAD_BALANCER_SERVICE, serviceRestartValidationFilter);
        c.router.filter(ServiceConstants.KIND_LOAD_BALANCER_SERVICE, serviceRollbackValidationFilter);
        c.router.filter(ServiceConstants.KIND_LOAD_BALANCER_SERVICE, serviceSetServiceLinksValidationFilter);
        c.router.filter(ServiceConstants.KIND_SCALING_GROUP_SERVICE, serviceAddRemoveLinkServiceValidationFilter);
        c.router.filter(ServiceConstants.KIND_SCALING_GROUP_SERVICE, serviceRestartValidationFilter);
        c.router.filter(ServiceConstants.KIND_SCALING_GROUP_SERVICE, serviceRollbackValidationFilter);
        c.router.filter(ServiceConstants.KIND_SCALING_GROUP_SERVICE, serviceSetServiceLinksValidationFilter);
        c.router.filter(ServiceConstants.KIND_SELECTOR_SERVICE,selectorServiceCreateValidationFilter);
        c.router.filter(ServiceConstants.KIND_SERVICE, selectorServiceCreateValidationFilter);
        c.router.filter(ServiceConstants.KIND_SERVICE, serviceAddRemoveLinkServiceValidationFilter);
        c.router.filter(ServiceConstants.KIND_SERVICE, serviceRestartValidationFilter);
        c.router.filter(ServiceConstants.KIND_SERVICE, serviceRollbackValidationFilter);
        c.router.filter(ServiceConstants.KIND_SERVICE, serviceSetServiceLinksValidationFilter);
        c.router.filter(ServiceEvent.class, new ServiceEventFilter(f.objectManager, d.agentDao, d.serviceDao));
        c.router.filter(Stack.class, serviceStackNetworkDriverFilter);
        c.router.filter(Stack.class, serviceStackStorageDriverFilter);
        c.router.filter(Stack.class, stackCreateValidationFilter);
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
                    new RegisterScriptHandler(c.registrationAuthTokenManager),
                    new TransformInspect(c.dockerTransformer, f.jsonMapper))),
                new BodyParserRequestHandler(f.schemaJsonMapper),
                new ConfigurableRequestOptionsParser(),
                new RequestReRouterHandler(),
                noAuthenticationProxy(),
                new ApiAuthenticator(f.objectManager, c.transformationService, d.accountDao),
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
        c.router.resourceManager(ServiceProxy.class, new ServiceProxyManager(d.instanceDao, containerProxyActionHandler));
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
        ResourceChangeEventProcessor resourceChangeEventProcessor = new ResourceChangeEventProcessor(c.locator, responseConverter, f.schemaJsonMapper);
        JettyWebSocketSubcriptionHandler jettyWebSocketSubcriptionHandler = new JettyWebSocketSubcriptionHandler(f.jsonMapper,
                f.eventService,
                f.retryTimeoutService,
                f.executorService,
                Arrays.asList(resourceChangeEventProcessor));

        c.router.resourceManager(Subscribe.class, new SubscribeManager(jettyWebSocketSubcriptionHandler));
        c.router.resourceManager(Publish.class, new PublishManager(f.objectManager, f.eventService));
    }

    private void setupApiCommon() {
        responseConverter = new ResponseObjectConverter(f.metaDataManager, f.objectManager, c.locator);
        referenceValidator = new ResourceManagerReferenceValidator(c.locator, responseConverter);

        Versions v = new Versions();
        v.setVersions(new HashSet<>(Arrays.asList(
                "v1",
                "v2-beta",
                "v2"
                )));
        v.setLatest("v2");
        v.setRootVersion("v1");
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

    private HtmlResponseWriter htmlResponseWriter() {
        JacksonMapper jacksonMapper = new JacksonMapper();
        jacksonMapper.setEscapeForwardSlashes(true);
        jacksonMapper.init();

        HtmlResponseWriter htmlWriter = new HtmlResponseWriter(jacksonMapper, new ConfigBasedHtmlTemplate());
        return htmlWriter;
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
        apiRequestFilterDelegate = new ApiRequestFilterDelegate(versions, new ApiRequestParser(), c.router,
                c.schemaFactories, f.idFormatter);
    }

    public ApiRequestFilterDelegate getApiRequestFilterDelegate() {
        return apiRequestFilterDelegate;
    }

    public Versions getVersions() {
        return versions;
    }

}