package io.cattle.platform.app;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.html.ConfigBasedHtmlTemplate;
import io.cattle.platform.api.parser.ApiRequestParser;
import io.cattle.platform.api.pubsub.manager.PublishManager;
import io.cattle.platform.api.pubsub.manager.SubscribeManager;
import io.cattle.platform.api.pubsub.model.Publish;
import io.cattle.platform.api.pubsub.model.Subscribe;
import io.cattle.platform.api.pubsub.subscribe.jetty.JettyWebSocketSubcriptionHandler;
import io.cattle.platform.api.resource.ExtensionResourceManagerLocator;
import io.cattle.platform.api.resource.jooq.DefaultJooqResourceManager;
import io.cattle.platform.api.schema.FileSchemaFactory;
import io.cattle.platform.api.settings.manager.SettingManager;
import io.cattle.platform.api.settings.model.ActiveSetting;
import io.cattle.platform.api.utils.ApiSettings;
import io.cattle.platform.configitem.api.manager.ConfigContentManager;
import io.cattle.platform.configitem.api.model.ConfigContent;
import io.cattle.platform.configitem.context.dao.impl.MetaDataInfoDaoImpl;
import io.cattle.platform.configitem.context.impl.ConfigUrlInfoFactory;
import io.cattle.platform.configitem.registry.impl.ConfigItemRegistryImpl;
import io.cattle.platform.configitem.server.agentinclude.impl.AgentIncludeConfigItemFactoryImpl;
import io.cattle.platform.configitem.server.agentinclude.impl.AgentIncludeMapImpl;
import io.cattle.platform.configitem.server.impl.ConfigItemServerImpl;
import io.cattle.platform.configitem.server.model.impl.GenericConfigItemFactory;
import io.cattle.platform.configitem.server.model.impl.MetadataConfigItemFactory;
import io.cattle.platform.configitem.server.model.impl.PSKConfigItemFactory;
import io.cattle.platform.configitem.server.task.ItemMigrationTask;
import io.cattle.platform.configitem.server.task.ItemSourceVersionSyncTask;
import io.cattle.platform.configitem.server.task.ItemSyncTask;
import io.cattle.platform.configitem.server.template.impl.DefaultTemplateLoader;
import io.cattle.platform.configitem.server.template.impl.FreemarkerTemplateLoader;
import io.cattle.platform.configitem.server.template.impl.FreemarkerURLTemplateLoader;
import io.cattle.platform.configitem.server.template.impl.TemplateFactoryImpl;
import io.cattle.platform.configitem.spring.URLArrayFactory;
import io.cattle.platform.docker.api.ContainerLogsActionHandler;
import io.cattle.platform.docker.api.ContainerProxyActionHandler;
import io.cattle.platform.docker.api.DockerSocketProxyActionHandler;
import io.cattle.platform.docker.api.ExecActionHandler;
import io.cattle.platform.docker.api.container.ServiceProxyManager;
import io.cattle.platform.docker.api.model.ContainerExec;
import io.cattle.platform.docker.api.model.ContainerLogs;
import io.cattle.platform.docker.api.model.ContainerProxy;
import io.cattle.platform.docker.api.model.DockerBuild;
import io.cattle.platform.docker.api.model.HostAccess;
import io.cattle.platform.docker.api.model.ServiceProxy;
import io.cattle.platform.docker.machine.api.MachineConfigLinkHandler;
import io.cattle.platform.engine.process.StateTransition;
import io.cattle.platform.extension.ExtensionImplementation;
import io.cattle.platform.extension.ExtensionPoint;
import io.cattle.platform.extension.api.dot.DotMaker;
import io.cattle.platform.extension.api.manager.ExtensionManagerApi;
import io.cattle.platform.extension.api.manager.ProcessDefinitionApiManager;
import io.cattle.platform.extension.api.manager.ResourceDefinitionManager;
import io.cattle.platform.extension.api.model.ProcessDefinitionApi;
import io.cattle.platform.extension.api.model.ResourceDefinition;
import io.cattle.platform.extension.impl.ExtensionManagerImpl;
import io.cattle.platform.framework.encryption.request.handler.TransformationHandler;
import io.cattle.platform.host.stats.api.ContainerStatsLinkHandler;
import io.cattle.platform.host.stats.api.HostStatsLinkHandler;
import io.cattle.platform.host.stats.api.ServiceContainerStatsLinkHandler;
import io.cattle.platform.host.stats.api.StatsLinkHandler;
import io.cattle.platform.iaas.api.account.AccountDeactivateActionHandler;
import io.cattle.platform.iaas.api.auth.dao.impl.CredentialUniqueFilter;
import io.cattle.platform.iaas.api.auth.dao.impl.PasswordDaoImpl;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.azure.AzureConfig;
import io.cattle.platform.iaas.api.auth.integration.ldap.OpenLDAP.OpenLDAPConfig;
import io.cattle.platform.iaas.api.auth.integration.ldap.ad.ADConfig;
import io.cattle.platform.iaas.api.auth.integration.local.ChangeSecretActionHandler;
import io.cattle.platform.iaas.api.auth.integration.local.LocalAuthConfig;
import io.cattle.platform.iaas.api.auth.projects.Member;
import io.cattle.platform.iaas.api.credential.ApiKeyCertificateDownloadLinkHandler;
import io.cattle.platform.iaas.api.credential.SshKeyPemDownloadLinkHandler;
import io.cattle.platform.iaas.api.host.HostEvacuateActionHandler;
import io.cattle.platform.iaas.api.snapshot.SnapshotBackupActionHandler;
import io.cattle.platform.iaas.api.volume.VolumeSnapshotActionHandler;
import io.cattle.platform.object.meta.TypeSet;
import io.cattle.platform.storage.api.filter.ExternalTemplateInstanceFilter;
import io.cattle.platform.systemstack.service.UpgradeManager;
import io.cattle.platform.systemstack.task.UpgradeScheduleTask;
import io.cattle.platform.task.action.TaskExecuteActionHandler;
import io.cattle.platform.vm.api.InstanceConsoleActionHandler;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.SubSchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.response.impl.ResourceOutputFilterManagerImpl;
import io.github.ibuildthecloud.gdapi.servlet.ApiRequestFilterDelegate;
import io.github.ibuildthecloud.gdapi.version.Versions;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("deprecation")
@Configuration
@ComponentScans({
        @ComponentScan("io.cattle.platform.configitem.context.impl"),
        @ComponentScan("io.cattle.platform.servicediscovery.api.filter"),
        @ComponentScan("io.cattle.platform.servicediscovery.api.action"),
        @ComponentScan("io.cattle.platform.servicediscovery.api.service.impl"),
        @ComponentScan("io.cattle.platform.servicediscovery.api.export")
})
public class ApiServerConfig {

    @Bean
    TypeSet ConfigItemApiTypes() {
        TypeSet set = new TypeSet("ConfigItemApiTypes");
        set.setTypeClasses(Arrays.<Class<?>>asList(
                ConfigContent.class
                ));
        return set;
    }

    @Bean
    ConfigContentManager ConfigContentManager() {
        return new ConfigContentManager();
    }

    @Bean
    TypeSet ApiPubSubTypeSet() {
        TypeSet set = new TypeSet("ApiPubSubTypeSet");
        set.setTypeClasses(Arrays.asList(
                Subscribe.class,
                Publish.class
                ));
        return set;
    }

    @Bean
    SubscribeManager SubscribeManager() {
        return new SubscribeManager();
    }

    @Bean
    PublishManager PublishManager() {
        return new PublishManager();
    }

    @Bean
    JettyWebSocketSubcriptionHandler JettyWebSocketSubcriptionHandler(@Qualifier("EventExecutorService") ExecutorService es) {
        io.cattle.platform.api.pubsub.subscribe.jetty.JettyWebSocketSubcriptionHandler handler = new JettyWebSocketSubcriptionHandler();
        handler.setExecutorService(es);
        return handler;
    }

    @Bean
    Versions Versions() {
        io.github.ibuildthecloud.gdapi.version.Versions v = new Versions();
        v.setVersions(new HashSet<>(Arrays.asList(
                "v1",
                "v2-beta"
                )));
        v.setLatest("v2-beta");
        v.setRootVersion("v1");
        return v;
    }

    @Bean
    ApiRequestFilterDelegate ApiRequestFilterDelegate(@Qualifier("DefaultIdFormatter") IdFormatter idF,
            @Qualifier("v1-base-factory") SchemaFactory v1, @Qualifier("CoreSchemaFactory") SchemaFactory core) {
        Map<String, SchemaFactory> factories = new HashMap<>();
        factories.put("v1", v1);
        factories.put("v2-beta", core);
        io.github.ibuildthecloud.gdapi.servlet.ApiRequestFilterDelegate delegate = new ApiRequestFilterDelegate();
        delegate.setSchemaFactories(factories);
        delegate.setIdFormatter(idF);
        return delegate;
    }

    @Bean
    ExtensionResourceManagerLocator ExtensionResourceManagerLocator(@Qualifier("CoreSchemaFactory") SchemaFactory factory) {
        ExtensionResourceManagerLocator locator = new ExtensionResourceManagerLocator();
        locator.setSchemaFactory(factory);
        return locator;
    }

    @Bean
    DefaultJooqResourceManager DefaultResourceManager() {
        return new DefaultJooqResourceManager();
    }

    @Bean
    ConfigBasedHtmlTemplate ConfigBasedHtmlTemplate() {
        io.cattle.platform.api.html.ConfigBasedHtmlTemplate template = new ConfigBasedHtmlTemplate();
        template.setSettings(new ApiSettings());
        return template;
    }

    @Bean
    ApiRequestParser ApiRequestParser() {
        return new ApiRequestParser();
    }

    @Bean
    SubSchemaFactory noop(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory) {
        /*
         * This is here only to ensure that there are two SchemaFactories in the context
         * to make autowiring fail
         */
        SubSchemaFactory factory = new SubSchemaFactory();
        factory.setSchemaFactory(coreSchemaFactory);
        factory.setId("noop");
        return factory;
    }

    @Bean
    ResourceOutputFilterManagerImpl ResourceOutputFilterManagerImpl(@Qualifier("CoreSchemaFactory") SchemaFactory factory) {
        ResourceOutputFilterManagerImpl manager = new ResourceOutputFilterManagerImpl();
        manager.setBaseSchemaFactory(factory);
        return manager;
    }

    @Bean
    TransformationHandler TransformationHandler() {
        return new TransformationHandler();
    }

    @Bean
    MetaDataInfoDaoImpl MetaDataInfoDaoImpl() {
        return new MetaDataInfoDaoImpl();
    }

    @Bean
    ConfigItemServerImpl ConfigItemServerImpl() {
        return new ConfigItemServerImpl();
    }

    @Bean
    ConfigItemRegistryImpl ConfigItemRegistryImpl() {
        return new ConfigItemRegistryImpl();
    }

    @Bean
    PSKConfigItemFactory PSKConfigItemFactory() {
        return new PSKConfigItemFactory();
    }

    @Bean
    MetadataConfigItemFactory MetadataConfigItemFactory() {
        return new MetadataConfigItemFactory();
    }

    @Bean
    URLArrayFactory GenericResources() {
        URLArrayFactory factory = new URLArrayFactory();
        factory.setLocations(new String[] {
            "classpath*:/config-content/**/*"
        });
        return factory;
    }

    @Bean
    ConfigUrlInfoFactory ConfigUrlInfoFactory() {
        return new ConfigUrlInfoFactory();
    }

    @Bean
    GenericConfigItemFactory GenericConfigItemFactory(@Qualifier("GenericResources") URL[] resources,
            @Qualifier("ConfigUrlInfoFactory") Callable<byte[]> callback) {
        Map<String, Callable<byte[]>> additional = new HashMap<>();
        additional.put("agent-instance-startup", callback);

        GenericConfigItemFactory factory = new GenericConfigItemFactory();
        factory.setRoot("config-content");
        factory.setDevRelativePath("../../../content/config-content");
        factory.setName("CommonConfigItems");
        factory.setResources(resources);
        factory.setAdditionalRevisionData(additional);
        return factory;
    }

    @Bean
    DefaultTemplateLoader DefaultTemplateLoader() {
        return new DefaultTemplateLoader();
    }

    @Bean
    FreemarkerTemplateLoader FreemarkerTemplateLoader() {
        return new FreemarkerTemplateLoader();
    }

    @Bean
    TemplateFactoryImpl TemplateFactoryImpl() {
        return new TemplateFactoryImpl();
    }

    @Bean
    freemarker.template.Configuration FreemarkerConfig() {
        freemarker.template.Configuration config = new freemarker.template.Configuration();
        config.setTemplateLoader(new FreemarkerURLTemplateLoader());
        config.setLocalizedLookup(false);
        config.setNumberFormat("computer");
        return config;
    }

    @Bean
    ItemSyncTask ItemSyncTask() {
        return new ItemSyncTask();
    }

    @Bean
    ItemMigrationTask ItemMigrationTask() {
        return new ItemMigrationTask();
    }

    @Bean
    ItemSourceVersionSyncTask ItemSourceVersionSyncTask() {
        return new ItemSourceVersionSyncTask();
    }

    @Bean
    UpgradeScheduleTask UpgradeScheduleTask() {
        return new UpgradeScheduleTask();
    }

    @Bean
    UpgradeManager UpgradeManager() {
        return new UpgradeManager();
    }

    @Bean
    URLArrayFactory AgentResources() {
        URLArrayFactory factory = new URLArrayFactory();
        factory.setLocations(new String[] {
            "classpath*:agent/agent-include/**/*"
        });
        return factory;
    }

    @Bean
    AgentIncludeConfigItemFactoryImpl AgentIncludeConfigItemFactoryImpl(@Qualifier("AgentResources") URL[] resources) {
        AgentIncludeConfigItemFactoryImpl factory = new AgentIncludeConfigItemFactoryImpl();
        factory.setFileRoot("../../../resources/content/agent/agent-include");
        factory.setRoot("agent");
        factory.setResources(resources);
        return factory;
    }

    @Bean
    AgentIncludeMapImpl AgentIncludeMapImpl() {
        return new AgentIncludeMapImpl();
    }

    @Bean
    ExecActionHandler ExecActionHandler() {
        return new ExecActionHandler();
    }

    @Bean
    ContainerLogsActionHandler ContainerLogsActionHandler() {
        return new ContainerLogsActionHandler();
    }

    @Bean
    ContainerProxyActionHandler ContainerProxyActionHandler() {
        return new ContainerProxyActionHandler();
    }

    @Bean
    DockerSocketProxyActionHandler DockerSocketProxyActionHandler() {
        return new DockerSocketProxyActionHandler();
    }

    @Bean
    ServiceProxyManager ServiceProxyManager() {
        return new ServiceProxyManager();
    }

    @Bean
    VolumeSnapshotActionHandler VolumeSnapshotActionHandler() {
        return new VolumeSnapshotActionHandler();
    }

    @Bean
    SnapshotBackupActionHandler SnapshotBackupActionHandler() {
        return new SnapshotBackupActionHandler();
    }

    @Bean
    TypeSet DockerApiTypes() {
        TypeSet typeSet = new TypeSet("DockerApiTypes");
        typeSet.setTypeClasses(Arrays.<Class<?>>asList(
                ContainerExec.class,
                ContainerLogs.class,
                ContainerProxy.class,
                ServiceProxy.class,
                HostAccess.class,
                DockerBuild.class
                ));
        return typeSet;
    }

    @Bean
    TypeSet ExtensionTypeSet() {
        TypeSet typeSet = new TypeSet("ExtensionTypeSet");
        typeSet.setTypeClasses(Arrays.<Class<?>>asList(
                ExtensionImplementation.class,
                ExtensionPoint.class,
                ProcessDefinitionApi.class,
                ResourceDefinition.class,
                StateTransition.class,
                Token.class,
                Member.class,
                Identity.class,
                ADConfig.class,
                OpenLDAPConfig.class,
                LocalAuthConfig.class,
                AzureConfig.class
                ));
        return typeSet;
    }

    @Bean
    ExtensionManagerApi ExtensionManagerApi() {
        return new ExtensionManagerApi();
    }

    @Bean
    ProcessDefinitionApiManager ProcessDefinitionApiManager() {
        return new ProcessDefinitionApiManager();
    }

    @Bean
    ResourceDefinitionManager ResourceDefinitionManager(ExtensionManagerImpl em) {
        return new ResourceDefinitionManager();
    }

    @Bean
    DotMaker DotMaker(ExtensionManagerImpl em) {
        return new DotMaker();
    }

    @Bean
    StatsLinkHandler StatsLinkHandler() {
        return new StatsLinkHandler();
    }

    @Bean
    HostStatsLinkHandler HostStatsLinkHandler() {
        return new HostStatsLinkHandler();
    }

    @Bean
    ContainerStatsLinkHandler ContainerStatsLinkHandler() {
        return new ContainerStatsLinkHandler();
    }

    @Bean
    ServiceContainerStatsLinkHandler ServiceContainerStatsLinkHandler() {
        return new ServiceContainerStatsLinkHandler();
    }

    @Bean
    SshKeyPemDownloadLinkHandler SshKeyPemDownloadLinkHandler() {
        return new SshKeyPemDownloadLinkHandler();
    }

    @Bean
    ApiKeyCertificateDownloadLinkHandler ApiKeyCertificateDownloadLinkHandler() {
        return new ApiKeyCertificateDownloadLinkHandler();
    }

    @Bean
    MachineConfigLinkHandler MachineConfigLinkHandler() {
        return new MachineConfigLinkHandler();
    }

    @Bean
    ChangeSecretActionHandler ChangeSecretActionHandler() {
        return new ChangeSecretActionHandler();
    }

    @Bean
    PasswordDaoImpl PasswordDaoImpl() {
        return new PasswordDaoImpl();
    }

    @Bean
    CredentialUniqueFilter CredentialUniqueFilter(@Qualifier("CoreSchemaFactory") SchemaFactory factory) {
        CredentialUniqueFilter filter = new CredentialUniqueFilter();
        filter.setSchemaFactory(factory);
        return filter;
    }

    @Bean
    AccountDeactivateActionHandler AccountDeactivateActionHandler() {
        return new AccountDeactivateActionHandler();
    }

    @Bean
    HostEvacuateActionHandler HostEvacuateActionHandler() {
        return new HostEvacuateActionHandler();
    }

    @Bean
    TypeSet DynamicCoreModel() {
        TypeSet typeSet = new TypeSet("DynamicCoreModel");
        typeSet.setTypeClasses(Arrays.<Class<?>>asList(
                ActiveSetting.class
                ));
        return typeSet;
    }

    @Bean
    SettingManager SettingManager() {
        return new SettingManager();
    }

    @Bean
    ExternalTemplateInstanceFilter ExternalTemplateInstanceFilter(@Qualifier("CoreSchemaFactory") SchemaFactory schemaFactory) {
        ExternalTemplateInstanceFilter filter = new ExternalTemplateInstanceFilter();
        filter.setSchemaFactory(schemaFactory);
        return filter;
    }

    @Bean
    TaskExecuteActionHandler TaskExecuteActionHandler() {
        return new TaskExecuteActionHandler();
    }

    @Bean
    InstanceConsoleActionHandler InstanceConsoleActionHandler() {
        return new InstanceConsoleActionHandler();
    }

    @Bean(name = "v1-admin-factory")
    SchemaFactory v1AdminFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/admin.ser");
        return factory;
    }

    @Bean(name = "v1-member-factory")
    SchemaFactory v1MemberFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/member.ser");
        return factory;
    }

    @Bean(name = "v1-owner-factory")
    SchemaFactory v1OwnerFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/owner.ser");
        return factory;
    }

    @Bean(name = "v1-project-factory")
    SchemaFactory v1ProjectFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/project.ser");
        return factory;
    }

    @Bean(name = "v1-projectadmin-factory")
    SchemaFactory v1ProjectAdminFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/projectadmin.ser");
        return factory;
    }

    @Bean(name = "v1-readAdmin-factory")
    SchemaFactory v1ReadAdminFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/readAdmin.ser");
        return factory;
    }

    @Bean(name = "v1-readonly-factory")
    SchemaFactory v1ReadOnlyFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/readonly.ser");
        return factory;
    }

    @Bean(name = "v1-register-factory")
    SchemaFactory v1RegisterFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/register.ser");
        return factory;
    }

    @Bean(name = "v1-restricted-factory")
    SchemaFactory v1RestrictedFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/restricted.ser");
        return factory;
    }

    @Bean(name = "v1-service-factory")
    SchemaFactory v1ServiceFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/service.ser");
        return factory;
    }

    @Bean(name = "v1-token-factory")
    SchemaFactory v1TokenFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/token.ser");
        return factory;
    }

    @Bean(name = "v1-user-factory")
    SchemaFactory v1UserFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/user.ser");
        return factory;
    }

    @Bean(name = "v1-agent-factory")
    SchemaFactory v1AgentFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/agent.ser");
        return factory;
    }

    @Bean(name = "v1-agentRegister-factory")
    SchemaFactory v1AgentRegisterFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/agentRegister.ser");
        return factory;
    }

    @Bean(name = "v1-base-factory")
    SchemaFactory v1BaseFactory() {
        FileSchemaFactory factory = new FileSchemaFactory();
        factory.setFile("schema/v1/base.ser");
        return factory;
    }
}
