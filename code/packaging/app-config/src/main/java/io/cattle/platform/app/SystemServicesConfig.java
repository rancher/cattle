package io.cattle.platform.app;

import io.cattle.iaas.healthcheck.service.impl.AgentHealthcheckHostLookup;
import io.cattle.iaas.healthcheck.service.impl.AgentHealthcheckInstancesLookup;
import io.cattle.iaas.healthcheck.service.impl.HealthcheckCleanupMonitorImpl;
import io.cattle.iaas.healthcheck.service.impl.HealthcheckServiceImpl;
import io.cattle.iaas.healthcheck.service.impl.HostHealthcheckHostLookup;
import io.cattle.iaas.healthcheck.service.impl.HostMapHealthcheckInstancesLookup;
import io.cattle.iaas.healthcheck.service.impl.UpgradeCleanupMonitorImpl;
import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.agent.impl.AgentLocatorImpl;
import io.cattle.platform.agent.instance.factory.impl.AgentInstanceFactoryImpl;
import io.cattle.platform.agent.instance.serialization.AgentInstanceAuthObjectPostProcessor;
import io.cattle.platform.agent.instance.service.AgentMetadataService;
import io.cattle.platform.api.formatter.DefaultIdFormatter;
import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.async.retry.impl.RetryTimeoutServiceImpl;
import io.cattle.platform.configitem.version.dao.impl.ConfigItemStatusDaoImpl;
import io.cattle.platform.configitem.version.impl.ConfigItemStatusManagerImpl;
import io.cattle.platform.configitem.version.impl.ConfigUpdatePublisher;
import io.cattle.platform.core.cache.DBCacheManager;
import io.cattle.platform.core.cleanup.BadDataCleanup;
import io.cattle.platform.core.cleanup.CleanupTaskInstances;
import io.cattle.platform.core.cleanup.TableCleanup;
import io.cattle.platform.docker.machine.launch.AuthServiceLauncher;
import io.cattle.platform.docker.machine.launch.CatalogLauncher;
import io.cattle.platform.docker.machine.launch.ComposeExecutorLauncher;
import io.cattle.platform.docker.machine.launch.MachineDriverLoader;
import io.cattle.platform.docker.machine.launch.MachineLauncher;
import io.cattle.platform.docker.machine.launch.TelemetryLauncher;
import io.cattle.platform.docker.machine.launch.WebhookServiceLauncher;
import io.cattle.platform.docker.machine.process.MachinePreCreate;
import io.cattle.platform.docker.process.account.DockerAccountCreate;
import io.cattle.platform.docker.process.serializer.DockerContainerSerializer;
import io.cattle.platform.docker.transform.DockerTransformerImpl;
import io.cattle.platform.engine.eventing.impl.ProcessEventListenerImpl;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.manager.impl.DefaultProcessManager;
import io.cattle.platform.engine.manager.impl.LoopManagerImpl;
import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.manager.impl.jooq.JooqProcessRecordDao;
import io.cattle.platform.engine.server.ProcessServer;
import io.cattle.platform.engine.server.impl.ProcessInstanceDispatcherImpl;
import io.cattle.platform.engine.task.ProcessReplayTask;
import io.cattle.platform.eventing.annotation.AnnotatedListenerRegistration;
import io.cattle.platform.eventing.memory.InMemoryEventService;
import io.cattle.platform.extension.dynamic.DynamicExtensionHandler;
import io.cattle.platform.extension.dynamic.dao.impl.ExternalHandlerDaoImpl;
import io.cattle.platform.extension.dynamic.impl.ExternalDynamicExtensionHandlerImpl;
import io.cattle.platform.extension.dynamic.process.ExternalHandlerActivate;
import io.cattle.platform.extension.impl.EMUtils;
import io.cattle.platform.extension.impl.ExtensionManagerImpl;
import io.cattle.platform.framework.encryption.EncryptionUtils;
import io.cattle.platform.framework.encryption.handler.impl.TransformationServiceImpl;
import io.cattle.platform.framework.encryption.impl.Aes256Encrypter;
import io.cattle.platform.framework.encryption.impl.PasswordHasher;
import io.cattle.platform.framework.encryption.impl.Sha256Hasher;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.framework.secret.SecretsServiceImpl;
import io.cattle.platform.ha.monitor.dao.impl.PingInstancesMonitorDaoImpl;
import io.cattle.platform.ha.monitor.impl.PingInstancesMonitorImpl;
import io.cattle.platform.hazelcast.membership.DBDiscovery;
import io.cattle.platform.hazelcast.membership.dao.ClusterMembershipDAO;
import io.cattle.platform.hazelcast.membership.dao.impl.ClusterMembershipDAOImpl;
import io.cattle.platform.host.service.HostApiRSAKeyProvider;
import io.cattle.platform.host.service.impl.HostApiServiceImpl;
import io.cattle.platform.iaas.api.auditing.AuditServiceImpl;
import io.cattle.platform.iaas.api.auditing.dao.impl.AuditLogDaoImpl;
import io.cattle.platform.jmx.JmxPublisherFactory;
import io.cattle.platform.liquibase.Loader;
import io.cattle.platform.lock.impl.LockDelegatorImpl;
import io.cattle.platform.lock.impl.LockManagerImpl;
import io.cattle.platform.lock.provider.impl.InMemoryLockProvider;
import io.cattle.platform.network.impl.NetworkServiceImpl;
import io.cattle.platform.object.ObjectDefaultsProvider;
import io.cattle.platform.object.defaults.JsonDefaultsProvider;
import io.cattle.platform.object.impl.DefaultObjectManager;
import io.cattle.platform.object.impl.TransactionDelegateImpl;
import io.cattle.platform.object.meta.TypeSet;
import io.cattle.platform.object.meta.impl.DefaultObjectMetaDataManager;
import io.cattle.platform.object.monitor.impl.ResourceMonitorImpl;
import io.cattle.platform.object.postinit.ObjectDataPostInstantiationHandler;
import io.cattle.platform.object.postinit.ObjectDefaultsPostInstantiationHandler;
import io.cattle.platform.object.postinit.SetPropertiesPostInstantiationHandler;
import io.cattle.platform.object.postinit.SpecialFieldsPostInstantiationHandler;
import io.cattle.platform.object.postinit.UUIDPostInstantiationHandler;
import io.cattle.platform.object.process.impl.DefaultObjectProcessManager;
import io.cattle.platform.object.process.impl.ObjectExecutionExceptionHandler;
import io.cattle.platform.object.purge.impl.PurgeMonitorImpl;
import io.cattle.platform.object.purge.impl.RemoveMonitorImpl;
import io.cattle.platform.object.serialization.impl.DefaultObjectSerializerFactoryImpl;
import io.cattle.platform.object.util.CommonsConverterStartup;
import io.cattle.platform.process.common.spring.GenericResourceProcessDefinitionCollector;
import io.cattle.platform.process.containerevent.ContainerEventCreate;
import io.cattle.platform.process.host.HostRemoveMonitorImpl;
import io.cattle.platform.process.image.PullTaskCreate;
import io.cattle.platform.process.monitor.EventNotificationChangeMonitor;
import io.cattle.platform.process.progress.ProcessProgressImpl;
import io.cattle.platform.register.dao.impl.RegisterDaoImpl;
import io.cattle.platform.resource.pool.impl.ResourcePoolManagerImpl;
import io.cattle.platform.resource.pool.mac.MacAddressGeneratorFactory;
import io.cattle.platform.resource.pool.mac.MacAddressPrefixGeneratorFactory;
import io.cattle.platform.resource.pool.port.EnvironmentPortGeneratorFactory;
import io.cattle.platform.resource.pool.port.HostPortGeneratorFactory;
import io.cattle.platform.resource.pool.subnet.SubnetAddressGeneratorFactory;
import io.cattle.platform.sample.data.SampleDataStartupV10;
import io.cattle.platform.sample.data.SampleDataStartupV11;
import io.cattle.platform.sample.data.SampleDataStartupV12;
import io.cattle.platform.sample.data.SampleDataStartupV13;
import io.cattle.platform.sample.data.SampleDataStartupV14;
import io.cattle.platform.sample.data.SampleDataStartupV15;
import io.cattle.platform.sample.data.SampleDataStartupV16;
import io.cattle.platform.sample.data.SampleDataStartupV17;
import io.cattle.platform.sample.data.SampleDataStartupV3;
import io.cattle.platform.sample.data.SampleDataStartupV5;
import io.cattle.platform.sample.data.SampleDataStartupV6;
import io.cattle.platform.sample.data.SampleDataStartupV8;
import io.cattle.platform.sample.data.SampleDataStartupV9;
import io.cattle.platform.service.account.SystemRoleObjectPostProcessor;
import io.cattle.platform.service.launcher.ServiceAccountCreateStartup;
import io.cattle.platform.servicediscovery.deployment.lookups.AgentServiceDeploymentUnitLookup;
import io.cattle.platform.servicediscovery.deployment.lookups.DeploymentUnitImplLookup;
import io.cattle.platform.servicediscovery.deployment.lookups.InstanceDeploymentUnitLookup;
import io.cattle.platform.servicediscovery.deployment.lookups.VolumeDeploymentUnitLookup;
import io.cattle.platform.servicediscovery.service.impl.ServiceDiscoveryServiceImpl;
import io.cattle.platform.servicediscovery.service.lookups.AgentServiceLookup;
import io.cattle.platform.servicediscovery.service.lookups.DeploymentUnitServiceLookup;
import io.cattle.platform.servicediscovery.service.lookups.GenericObjectLookup;
import io.cattle.platform.servicediscovery.service.lookups.GlobalHostActivateServiceLookup;
import io.cattle.platform.servicediscovery.service.lookups.InstanceServiceLookup;
import io.cattle.platform.servicediscovery.service.lookups.ServiceImplLookup;
import io.cattle.platform.spring.resource.SpringUrlListFactory;
import io.cattle.platform.storage.ImageCredentialLookup;
import io.cattle.platform.storage.impl.DockerImageCredentialLookup;
import io.cattle.platform.storage.service.impl.StorageServiceImpl;
import io.cattle.platform.systemstack.catalog.impl.CatalogServiceImpl;
import io.cattle.platform.systemstack.listener.SystemStackUpdate;
import io.cattle.platform.systemstack.service.ProjectTemplateService;
import io.cattle.platform.task.eventing.impl.TaskManagerEventListenerImpl;
import io.cattle.platform.task.impl.TaskManagerImpl;
import io.cattle.platform.token.impl.JwtTokenServiceImpl;
import io.cattle.platform.vm.process.ServiceVirtualMachinePreCreate;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.json.JacksonMapper;
import io.github.ibuildthecloud.gdapi.model.Transformer;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScans({
    @ComponentScan("io.cattle.platform.core.dao.impl"),
    @ComponentScan("io.cattle.iaas.healthcheck.process"),
    @ComponentScan("io.cattle.platform.register.process"),
    @ComponentScan("io.cattle.platform.agent.instance.process"),
    @ComponentScan("io.cattle.platform.service.revision"),
    @ComponentScan("io.cattle.platform.servicediscovery.process"),
    @ComponentScan("io.cattle.platform.compose"),
    @ComponentScan("io.cattle.platform.systemstack.process"),
    @ComponentScan("io.cattle.platform.process")
})
public class SystemServicesConfig {

    @Bean
    ActivityService activityService() {
        return new ActivityService();
    }

    @Bean
    AgentInstanceFactoryImpl AgentInstanceFactory() {
        return new AgentInstanceFactoryImpl();
    }

    @Bean
    AgentInstanceAuthObjectPostProcessor agentInstanceAuthObjectPostProcessor() {
        return new AgentInstanceAuthObjectPostProcessor();
    }

    @Bean
    SystemRoleObjectPostProcessor SystemRoleObjectPostProcessor() {
        return new SystemRoleObjectPostProcessor();
    }

    @Bean
    DockerContainerSerializer DockerContainerSerializer() {
        return new DockerContainerSerializer();
    }

    @Bean
    AgentLocatorImpl AgentLocator() {
        return new AgentLocatorImpl();
    }

    @Bean
    DefaultIdFormatter DefaultIdFormatter(@Qualifier("CoreSchemaFactory") SchemaFactory factory) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("stack", "st");
        mapping.put("secret", "se");

        DefaultIdFormatter idF = new DefaultIdFormatter();
        idF.setSchemaFactory(factory);
        idF.setPlainTypes(new HashSet<>(Arrays.asList("typeDocumentation", "scripts")));
        idF.setTypeMappings(mapping);

        return idF;
    }

    @Bean
    RetryTimeoutService RetryTimeoutService(@Qualifier("EventExecutorService") ExecutorService es) {
        RetryTimeoutServiceImpl rts = new RetryTimeoutServiceImpl();
        rts.setExecutorService(es);
        return rts;
    }

    @Bean
    TableCleanup DatabaseCleanupService() {
        return new TableCleanup();
    }

    @Bean
    BadDataCleanup BadDataCleanup() {
        return new BadDataCleanup();
    }

    @Bean
    Sha256Hasher sha256Hasher(ExtensionManagerImpl em) {
        return EMUtils.add(em, Transformer.class, new Sha256Hasher(), "SHA256");
    }

    @Bean
    PasswordHasher passwordHasher(ExtensionManagerImpl em) {
        return EMUtils.add(em, Transformer.class, new PasswordHasher());
    }

    @Bean
    Aes256Encrypter aes256Encrypter(ExtensionManagerImpl em) {
        return EMUtils.add(em, Transformer.class, new Aes256Encrypter(), "AES256");
    }

    @SuppressWarnings("unchecked")
    @Bean
    TransformationService transformationService(ExtensionManagerImpl em) {
        TransformationServiceImpl ts = new TransformationServiceImpl();
        ts.setTransformers((Map<String, Transformer>)(Map<?, ?>)em.map("transformer"));
        return ts;
    }

    @Bean
    EncryptionUtils encryptionUtils() {
        return new EncryptionUtils();
    }

    @Bean
    ConfigUpdatePublisher configUpdatePublisher() {
        return new ConfigUpdatePublisher();
    }

    @Bean
    ConfigItemStatusManagerImpl configItemStatusManagerImpl() {
        return new ConfigItemStatusManagerImpl();
    }

    @Bean
    ConfigItemStatusDaoImpl configItemStatusDaoImpl() {
        return new ConfigItemStatusDaoImpl();
    }

    @Bean
    DBCacheManager dbCacheManager() {
        return new DBCacheManager();
    }

    @Bean
    DockerTransformerImpl dockerTransformerImpl() {
        return new DockerTransformerImpl();
    }

    @Bean
    ImageCredentialLookup ImageCredentialLookup() {
        return new DockerImageCredentialLookup();
    }

    @Bean
    PullTaskCreate PullTaskCreate(ExtensionManagerImpl em) {
        return new PullTaskCreate();
    }

    @Bean
    InMemoryEventService EventService(@Qualifier("EventExecutorService") ExecutorService executorService) {
        InMemoryEventService eventService = new InMemoryEventService();
        eventService.setExecutorService(executorService);
        return eventService;
    }

    @Bean
    AnnotatedListenerRegistration AnnotatedListenerRegistration() {
        return new AnnotatedListenerRegistration();
    }

    @Bean
    ExternalDynamicExtensionHandlerImpl externalDynamicExtensionHandlerImpl(ExtensionManagerImpl em) {
        ExternalDynamicExtensionHandlerImpl edeh = new ExternalDynamicExtensionHandlerImpl();
        EMUtils.add(em, DynamicExtensionHandler.class, edeh);
        return edeh;
    }

    @Bean
    ExternalHandlerDaoImpl externalHandlerDaoImpl() {
        return new ExternalHandlerDaoImpl();
    }

    @Bean
    PingInstancesMonitorDaoImpl pingInstancesMonitorDaoImpl() {
        return new PingInstancesMonitorDaoImpl();
    }

    @Bean
    PingInstancesMonitorImpl pingInstancesMonitorImpl() {
        return new PingInstancesMonitorImpl();
    }

    @Bean
    HealthcheckServiceImpl healthcheckServiceImpl() {
        return new HealthcheckServiceImpl();
    }

    @Bean
    AgentHealthcheckHostLookup agentHealthcheckHostLookup() {
        return new AgentHealthcheckHostLookup();
    }

    @Bean
    HostHealthcheckHostLookup hostHealthcheckHostLookup() {
        return new HostHealthcheckHostLookup();
    }

    @Bean
    AgentHealthcheckInstancesLookup agentHealthcheckInstancesLookup() {
        return new AgentHealthcheckInstancesLookup();
    }

    @Bean
    HostMapHealthcheckInstancesLookup hostMapHealthcheckInstancesLookup() {
        return new HostMapHealthcheckInstancesLookup();
    }

    @Bean
    HealthcheckCleanupMonitorImpl healthcheckCleanupMonitorImpl() {
        return new HealthcheckCleanupMonitorImpl();
    }

    @Bean
    UpgradeCleanupMonitorImpl upgradeCleanupMonitorImpl() {
        return new UpgradeCleanupMonitorImpl();
    }

    @Bean
    HostApiServiceImpl hostApiServiceImpl() {
        return new HostApiServiceImpl();
    }

    @Bean
    JwtTokenServiceImpl jwtTokenServiceImpl() {
        return new JwtTokenServiceImpl();
    }

    @Bean
    HostApiRSAKeyProvider hostApiRSAKeyProvider() {
        return new HostApiRSAKeyProvider();
    }

    @Bean
    TypeSet HostOnlyTypeSet(ExtensionManagerImpl em) {
        TypeSet typeSet = new TypeSet("HostOnlyTypeSet");
        typeSet.setTypeNames(Arrays.asList(
                "hostOnlyNetwork,parent=network"
                ));
        EMUtils.add(em, TypeSet.class, typeSet, "HostOnlyTypeSet");
        return typeSet;
    }

    @Bean
    SpringUrlListFactory JmxTransConfig() {
        SpringUrlListFactory factory = new SpringUrlListFactory();
        factory.setResources(Arrays.asList(
                "classpath:jmxtrans.json",
                "classpath*:jmxtrans/*.json",
                "classpath:org/jmxtrans/embedded/config/jmxtrans-internals.json",
                "classpath:org/jmxtrans/embedded/config/jvm-sun-hotspot.json"
                ));
        return factory;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    JmxPublisherFactory jmxPublisherFactory(@Qualifier("JmxTransConfig") List urls) {
       JmxPublisherFactory factory = new JmxPublisherFactory();
       factory.setResources(urls);
       return factory;
    }

    @Bean
    DefaultObjectMetaDataManager DefaultObjectMetaDataManager(ExtensionManagerImpl em) {
        return new DefaultObjectMetaDataManager();
    }

    @Bean
    DefaultObjectManager defaultObjectManager(@Qualifier("LockingJooqConfiguration") io.cattle.platform.db.jooq.config.Configuration config,
            @Qualifier("CoreSchemaFactory") SchemaFactory schemaFactory) {
        DefaultObjectManager om = new DefaultObjectManager();
        om.setLockingConfiguration(config);
        om.setSchemaFactory(schemaFactory);
        return om;
    }

    @Bean
    SpecialFieldsPostInstantiationHandler specialFieldsPostInstantiationHandler() {
        return new SpecialFieldsPostInstantiationHandler();
    }

    @Bean
    JsonDefaultsProvider jsonDefaultsProvider(@Qualifier("CoreSchemaFactory") SchemaFactory schemaFactory) {
        io.cattle.platform.object.defaults.JsonDefaultsProvider provider = new JsonDefaultsProvider();
        provider.setDefaultPath("schema/defaults");
        provider.setDefaultOverridePath("schema/defaults/overrides");
        provider.setSchemaFactory(schemaFactory);
        return provider;
    }

    @Bean
    ObjectDefaultsPostInstantiationHandler objectDefaultsPostInstantiationHandler(JsonDefaultsProvider provider) {
        ObjectDefaultsPostInstantiationHandler handler = new ObjectDefaultsPostInstantiationHandler();
        handler.setDefaultProviders(Arrays.asList((ObjectDefaultsProvider)provider));
        return handler;
    }

    @Bean
    ObjectDataPostInstantiationHandler ObjectDataPostInstantiationHandler() {
        return new ObjectDataPostInstantiationHandler();
    }

    @Bean
    SetPropertiesPostInstantiationHandler setPropertiesPostInstantiationHandler() {
        return new SetPropertiesPostInstantiationHandler();
    }

    @Bean
    UUIDPostInstantiationHandler UUIDPostInstantiationHandler() {
        return new UUIDPostInstantiationHandler();
    }

    @Bean
    DefaultObjectSerializerFactoryImpl DefaultObjectSerializerFactoryImpl() {
        return new DefaultObjectSerializerFactoryImpl();
    }

    @Bean
    CommonsConverterStartup CommonsConverterStartup() {
        return new CommonsConverterStartup();
    }

    @Bean
    TransactionDelegateImpl TransactionDelegateImpl() {
        return new TransactionDelegateImpl();
    }

    @Bean
    PurgeMonitorImpl PurgeMonitorImpl() {
        return new PurgeMonitorImpl();
    }

    @Bean
    RemoveMonitorImpl RemoveMonitorImpl() {
        return new RemoveMonitorImpl();
    }

    @Bean
    HostRemoveMonitorImpl EvacuateMonitorImpl() {
        return new HostRemoveMonitorImpl();
    }

    @Bean
    ProcessReplayTask ProcessReplayTask() {
        return new ProcessReplayTask();
    }

    @Bean
    ProcessEventListenerImpl ProcessEventListenerImpl() {
        return new ProcessEventListenerImpl();
    }

    @Bean
    ProcessRecordDao JooqProcessRecordDao() {
        return new JooqProcessRecordDao();
    }

    @Bean
    ClusterMembershipDAO ClusterMembershipDAO() {
        return new ClusterMembershipDAOImpl();
    }

    @Bean
    ProcessServer ProcessServer() {
        return new ProcessServer();
    }

    @Bean
    LoopManager LoopManagerImpl() {
        return new LoopManagerImpl();
    }

    @Bean
    ProcessInstanceDispatcherImpl ProcessInstanceDispatcherImpl() {
        return new ProcessInstanceDispatcherImpl();
    }

    @Bean
    DefaultProcessManager DefaultProcessManager(ExtensionManagerImpl em) {
        return new DefaultProcessManager();
    }

    @Bean
    ObjectExecutionExceptionHandler ObjectExecutionExceptionHandler() {
        return new ObjectExecutionExceptionHandler();
    }

    @Bean
    DefaultObjectProcessManager DefaultObjectProcessManager(@Qualifier("CoreSchemaFactory") SchemaFactory schemaFactory) {
       DefaultObjectProcessManager manager = new DefaultObjectProcessManager();
       manager.setSchemaFactory(schemaFactory);
       return manager;
    }

    @Bean
    RegisterDaoImpl RegisterDaoImpl() {
        return new RegisterDaoImpl();
    }

    @Bean
    TypeSet RegisterTypeSet(ExtensionManagerImpl em) {
        TypeSet typeSet = new TypeSet("RegisterTypeSet");
        typeSet.setTypeNames(Arrays.asList(
                "register,parent=genericObject",
                "registrationToken,parent=credential"
                ));
        EMUtils.add(em, TypeSet.class, typeSet, "RegisterTypeSet");
        return typeSet;
    }

    @Bean
    ResourceMonitorImpl ResourceMonitorImpl() {
        return new ResourceMonitorImpl();
    }

    @Bean
    ResourcePoolManagerImpl ResourcePoolManagerImpl() {
        return  new ResourcePoolManagerImpl();
    }

    @Bean
    SubnetAddressGeneratorFactory SubnetAddressGeneratorFactory() {
        return new SubnetAddressGeneratorFactory();
    }

    @Bean
    MacAddressPrefixGeneratorFactory MacAddressPrefixGeneratorFactory() {
        return new MacAddressPrefixGeneratorFactory();
    }

    @Bean
    MacAddressGeneratorFactory MacAddressGeneratorFactory() {
        return new MacAddressGeneratorFactory();
    }

    @Bean
    HostPortGeneratorFactory HostPortGeneratorFactory() {
        return new HostPortGeneratorFactory();
    }

    @Bean
    EnvironmentPortGeneratorFactory EnvironmentPortGeneratorFactory() {
        return new EnvironmentPortGeneratorFactory();
    }

    @Bean
    SampleDataStartupV3 SampleDataStartupV3() {
        return new SampleDataStartupV3();
    }

    @Bean
    SampleDataStartupV6 SampleDataStartupV6() {
        return new SampleDataStartupV6();
    }

    @Bean
    SampleDataStartupV5 SampleDataStartupV5() {
        return new SampleDataStartupV5();
    }

    @Bean
    SampleDataStartupV17 SampleDataStartupV17() {
        return new SampleDataStartupV17();
    }

    @Bean
    SampleDataStartupV8 SampleDataStartupV8() {
        return new SampleDataStartupV8();
    }

    @Bean
    SampleDataStartupV9 SampleDataStartupV9() {
        return new SampleDataStartupV9();
    }

    @Bean
    SampleDataStartupV10 SampleDataStartupV10() {
        return new SampleDataStartupV10();
    }

    @Bean
    SampleDataStartupV11 SampleDataStartupV11() {
        return new SampleDataStartupV11();
    }

    @Bean
    SampleDataStartupV12 SampleDataStartupV12() {
        return new SampleDataStartupV12();
    }

    @Bean
    SampleDataStartupV13 SampleDataStartupV13() {
        return new SampleDataStartupV13();
    }

    @Bean
    SampleDataStartupV14 SampleDataStartupV14() {
        return new SampleDataStartupV14();
    }

    @Bean
    SampleDataStartupV15 SampleDataStartupV15() {
        return new SampleDataStartupV15();
    }

    @Bean
    SampleDataStartupV16 SampleDataStartupV16() {
        return new SampleDataStartupV16();
    }

    @Bean
    ServiceDiscoveryServiceImpl ServiceDiscoveryServiceImpl() {
        return new ServiceDiscoveryServiceImpl();
    }

    @Bean
    GlobalHostActivateServiceLookup GlobalHostActivateServiceLookup() {
        return new GlobalHostActivateServiceLookup();
    }

    @Bean
    DeploymentUnitServiceLookup DeploymentUnitServiceLookup() {
        return new DeploymentUnitServiceLookup();
    }

    @Bean
    InstanceServiceLookup InstanceServiceLookup() {
        return new InstanceServiceLookup();
    }

    @Bean
    AgentServiceLookup AgentServiceLookup() {
        return new AgentServiceLookup();
    }

    @Bean
    GenericObjectLookup GenericObjectLookup() {
        return new GenericObjectLookup();
    }

    @Bean
    ServiceImplLookup ServiceImplLookup() {
        return new ServiceImplLookup();
    }

    @Bean
    ProcessProgressImpl ProcessProgressImpl() {
        return new ProcessProgressImpl();
    }

    @Bean
    AuditServiceImpl AuditServiceImpl() {
        return new AuditServiceImpl();
    }

    @Bean
    AuditLogDaoImpl AuditLogDaoImpl() {
        return new AuditLogDaoImpl();
    }

    @Bean
    JacksonMapper JacksonMapper() {
        return new JacksonMapper();
    }

    @Bean
    StorageServiceImpl StorageServiceImpl(ExtensionManagerImpl em) {
        return new StorageServiceImpl();
    }

    @Bean
    InMemoryLockProvider LockProvider() {
        return new InMemoryLockProvider();
    }

    @Bean
    LockManagerImpl LockManagerImpl(@Qualifier("LockProvider") io.cattle.platform.lock.provider.LockProvider provider) {
        LockManagerImpl lock = new LockManagerImpl();
        lock.setLockProvider(provider);
        return lock;
    }

    @Bean
    LockDelegatorImpl LockDelegatorImpl(@Qualifier("EventExecutorService") ExecutorService es) {
        LockDelegatorImpl delegator = new LockDelegatorImpl();
        delegator.setExecutorService(es);
        return delegator;
    }

    @Bean
    SystemStackUpdate SystemStackUpdate() {
        return new SystemStackUpdate();
    }

    @Bean
    CatalogServiceImpl CatalogServiceImpl() {
        return new CatalogServiceImpl();
    }

    @Bean
    ProjectTemplateService ProjectTemplateService() {
        return new ProjectTemplateService();
    }

    @Bean
    TaskManagerImpl TaskManagerImpl() {
        return new TaskManagerImpl();
    }

    @Bean
    TaskManagerEventListenerImpl TaskManagerEventListenerImpl() {
        return new TaskManagerEventListenerImpl();
    }

    @Bean
    CleanupTaskInstances CleanupTaskInstances() {
        return new CleanupTaskInstances();
    }

    @Bean
    ServiceAccountCreateStartup ServiceAccountCreateStartup() {
        return new ServiceAccountCreateStartup();
    }

    @Bean
    NetworkServiceImpl NetworkServiceImpl() {
        return new NetworkServiceImpl();
    }

    @Bean
    AgentMetadataService AgentMetadataService() {
        return new AgentMetadataService();
    }

    @Bean
    DBDiscovery DBDiscovery(Loader loader) {
        return new DBDiscovery();
    }

    @Bean
    ContainerEventCreate ContainerEventCreate() {
        return new ContainerEventCreate();
    }

    @Bean
    EventNotificationChangeMonitor EventNotificationChangeMonitor() {
        return new EventNotificationChangeMonitor();
    }

    @Bean
    SecretsService SecretsService() {
        return new SecretsServiceImpl();
    }

    @Bean
    AgentServiceDeploymentUnitLookup AgentServiceDeploymentUnitLookup() {
        return new AgentServiceDeploymentUnitLookup();
    }

    @Bean
    DeploymentUnitImplLookup DeploymentUnitImplLookup() {
        return new DeploymentUnitImplLookup();
    }

    @Bean
    InstanceDeploymentUnitLookup InstanceServiceDeploymentUnitLookup() {
        return new InstanceDeploymentUnitLookup();
    }

    @Bean
    VolumeDeploymentUnitLookup VolumeDeploymentUnitLookup() {
        return new VolumeDeploymentUnitLookup();
    }

    @Bean
    GenericResourceProcessDefinitionCollector GenericResourceProcessDefinitionCollector() {
        return GenericResourceProcessDefinitionCollector();
    }

    @Bean
    DockerAccountCreate DockerAccountCreate() {
        return new DockerAccountCreate();
    }

    @Bean
    MachinePreCreate MachinePreCreate() {
        return new MachinePreCreate();
    }

    @Bean
    MachineDriverLoader MachineDriverLoader() {
        return new MachineDriverLoader();
    }

    @Bean
    MachineLauncher MachineLauncher() {
        return new MachineLauncher();
    }

    @Bean
    ComposeExecutorLauncher ComposeExecutorLauncher() {
        return new ComposeExecutorLauncher();
    }

    @Bean
    CatalogLauncher CatalogLauncher() {
        return new CatalogLauncher();
    }

    @Bean
    TelemetryLauncher TelemetryLauncher() {
        return new TelemetryLauncher();
    }

    @Bean
    AuthServiceLauncher AuthServiceLauncher() {
        return new AuthServiceLauncher();
    }

    @Bean
    WebhookServiceLauncher WebhookServiceLauncher() {
        return new WebhookServiceLauncher();
    }

    @Bean
    ExternalHandlerActivate ExternalHandlerActivate() {
        return new ExternalHandlerActivate();
    }

    @Bean
    ServiceVirtualMachinePreCreate ServiceVirtualMachinePreCreate() {
        return new ServiceVirtualMachinePreCreate();
    }

}
