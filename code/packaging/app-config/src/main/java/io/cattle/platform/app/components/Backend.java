package io.cattle.platform.app.components;

import io.cattle.iaas.healthcheck.process.ServiceEventCreate;
import io.cattle.iaas.healthcheck.service.impl.HealthcheckCleanupMonitorImpl;
import io.cattle.iaas.healthcheck.service.impl.UpgradeCleanupMonitorImpl;
import io.cattle.platform.agent.connection.simulator.AgentSimulator;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorConsoleProcessor;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorFailedProcessor;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorInstanceInspectProcessor;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorPingProcessor;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorStartStopProcessor;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.agent.instance.factory.impl.AgentInstanceFactoryImpl;
import io.cattle.platform.agent.server.ping.impl.PingMonitorImpl;
import io.cattle.platform.agent.server.resource.impl.AgentResourcesMonitor;
import io.cattle.platform.allocator.constraint.provider.AccountConstraintsProvider;
import io.cattle.platform.allocator.constraint.provider.AffinityConstraintsProvider;
import io.cattle.platform.allocator.constraint.provider.BaseConstraintsProvider;
import io.cattle.platform.allocator.constraint.provider.PortsConstraintProvider;
import io.cattle.platform.allocator.constraint.provider.VolumeAccessModeConstraintProvider;
import io.cattle.platform.allocator.service.AllocationHelperImpl;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.allocator.service.impl.AllocatorServiceImpl;
import io.cattle.platform.archaius.eventing.impl.ArchaiusEventListenerImpl;
import io.cattle.platform.backpopulate.BackPopulater;
import io.cattle.platform.backpopulate.impl.BackPopulaterImpl;
import io.cattle.platform.containersync.PingInstancesMonitor;
import io.cattle.platform.containersync.impl.PingInstancesMonitorImpl;
import io.cattle.platform.core.cleanup.CleanupTaskInstances;
import io.cattle.platform.core.cleanup.TableCleanup;
import io.cattle.platform.docker.machine.launch.AuthServiceLauncher;
import io.cattle.platform.docker.machine.launch.CatalogLauncher;
import io.cattle.platform.docker.machine.launch.ComposeExecutorLauncher;
import io.cattle.platform.docker.machine.launch.MachineDriverLoader;
import io.cattle.platform.docker.machine.launch.MachineLauncher;
import io.cattle.platform.docker.machine.launch.SecretsApiLauncher;
import io.cattle.platform.docker.machine.launch.TelemetryLauncher;
import io.cattle.platform.docker.machine.launch.WebhookServiceLauncher;
import io.cattle.platform.docker.machine.launch.WebsocketProxyLauncher;
import io.cattle.platform.docker.machine.process.MachinePreCreate;
import io.cattle.platform.engine.eventing.ProcessEventListener;
import io.cattle.platform.engine.eventing.impl.ProcessEventListenerImpl;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.process.ProcessRouter;
import io.cattle.platform.engine.task.ProcessReplayTask;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.AnnotatedListenerRegistration;
import io.cattle.platform.iaas.api.change.impl.ResourceChangeEventListenerImpl;
import io.cattle.platform.inator.process.InatorReconcileHandler;
import io.cattle.platform.lifecycle.AgentLifecycleManager;
import io.cattle.platform.lifecycle.AllocationLifecycleManager;
import io.cattle.platform.lifecycle.InstanceLifecycleManager;
import io.cattle.platform.lifecycle.K8sLifecycleManager;
import io.cattle.platform.lifecycle.NetworkLifecycleManager;
import io.cattle.platform.lifecycle.RestartLifecycleManager;
import io.cattle.platform.lifecycle.SecretsLifecycleManager;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.lifecycle.VirtualMachineLifecycleManager;
import io.cattle.platform.lifecycle.VolumeLifecycleManager;
import io.cattle.platform.lifecycle.impl.AgentLifecycleManagerImpl;
import io.cattle.platform.lifecycle.impl.AllocationLifecycleManagerImpl;
import io.cattle.platform.lifecycle.impl.InstanceLifecycleManagerImpl;
import io.cattle.platform.lifecycle.impl.K8sLifecycleManagerImpl;
import io.cattle.platform.lifecycle.impl.NetworkLifecycleManagerImpl;
import io.cattle.platform.lifecycle.impl.RestartLifecycleManagerImpl;
import io.cattle.platform.lifecycle.impl.SecretsLifecycleManagerImpl;
import io.cattle.platform.lifecycle.impl.ServiceLifecycleManagerImpl;
import io.cattle.platform.lifecycle.impl.VirtualMachineLifecycleManagerImpl;
import io.cattle.platform.lifecycle.impl.VolumeLifecycleManagerImpl;
import io.cattle.platform.loadbalancer.LoadBalancerService;
import io.cattle.platform.loadbalancer.impl.LoadBalancerServiceImpl;
import io.cattle.platform.loop.trigger.DeploymentUnitReconcileTrigger;
import io.cattle.platform.loop.trigger.ServiceReconcileTrigger;
import io.cattle.platform.metadata.service.MetadataTrigger;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.network.impl.NetworkServiceImpl;
import io.cattle.platform.object.purge.impl.PurgeMonitorImpl;
import io.cattle.platform.object.purge.impl.RemoveMonitorImpl;
import io.cattle.platform.process.account.AccountProcessManager;
import io.cattle.platform.process.agent.AgentHostStateUpdate;
import io.cattle.platform.process.agent.AgentProcessManager;
import io.cattle.platform.process.agent.AgentResourceRemove;
import io.cattle.platform.process.cache.ClearCacheHandler;
import io.cattle.platform.process.containerevent.ContainerEventCreate;
import io.cattle.platform.process.containerevent.ContainerEventPreCreate;
import io.cattle.platform.process.credential.CredentialProcessManager;
import io.cattle.platform.process.driver.DriverProcessManager;
import io.cattle.platform.process.dynamicschema.DynamicSchemaProcessManager;
import io.cattle.platform.process.externalevent.ExternalEventProcessManager;
import io.cattle.platform.process.generic.ActivateByDefault;
import io.cattle.platform.process.generic.SetRemovedFields;
import io.cattle.platform.process.host.HostProcessManager;
import io.cattle.platform.process.host.HostRemoveMonitorImpl;
import io.cattle.platform.process.hosttemplate.HosttemplateRemove;
import io.cattle.platform.process.image.PullTaskCreate;
import io.cattle.platform.process.instance.InstanceProcessManager;
import io.cattle.platform.process.instance.InstanceRemove;
import io.cattle.platform.process.instance.InstanceStart;
import io.cattle.platform.process.instance.InstanceStop;
import io.cattle.platform.process.instance.K8sProviderLabels;
import io.cattle.platform.process.mount.MountProcessManager;
import io.cattle.platform.process.mount.MountRemove;
import io.cattle.platform.process.network.NetworkProcessManager;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.process.progress.ProcessProgressImpl;
import io.cattle.platform.process.secret.SecretRemove;
import io.cattle.platform.process.service.ServiceProcessManager;
import io.cattle.platform.process.stack.K8sStackCreate;
import io.cattle.platform.process.stack.K8sStackFinishupgrade;
import io.cattle.platform.process.stack.K8sStackRemove;
import io.cattle.platform.process.stack.K8sStackRollback;
import io.cattle.platform.process.stack.K8sStackUpgrade;
import io.cattle.platform.process.storagepool.StoragePoolRemove;
import io.cattle.platform.process.subnet.SubnetCreate;
import io.cattle.platform.process.volume.VolumeProcessManager;
import io.cattle.platform.register.process.RegisterProcessManager;
import io.cattle.platform.sample.data.AbstractSampleData;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.servicediscovery.process.AccountLinkRemove;
import io.cattle.platform.servicediscovery.process.SelectorInstancePostListener;
import io.cattle.platform.servicediscovery.process.SelectorServiceCreatePostListener;
import io.cattle.platform.servicediscovery.process.StackProcessManager;
import io.cattle.platform.storage.ImageCredentialLookup;
import io.cattle.platform.storage.impl.DockerImageCredentialLookup;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.systemstack.catalog.impl.CatalogServiceImpl;
import io.cattle.platform.systemstack.loop.SystemStackTrigger;
import io.cattle.platform.systemstack.process.ProjecttemplateCreate;
import io.cattle.platform.systemstack.process.ScheduledUpgradeProcessManager;
import io.cattle.platform.systemstack.process.SystemStackProcessManager;
import io.cattle.platform.systemstack.service.ProjectTemplateService;
import io.cattle.platform.systemstack.service.UpgradeManager;
import io.cattle.platform.systemstack.task.RunScheduledTask;
import io.cattle.platform.systemstack.task.UpgradeScheduleTask;
import io.cattle.platform.task.eventing.TaskManagerEventListener;
import io.cattle.platform.task.eventing.impl.TaskManagerEventListenerImpl;
import io.cattle.platform.util.type.InitializationTask;
import io.cattle.platform.vm.process.ServiceVirtualMachinePreCreate;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Backend {

    private static final Logger CONSOLE_LOG = LoggerFactory.getLogger("ConsoleStatus");

    Framework f;
    Common c;
    DataAccess d;

    AgentInstanceFactory agentInstanceFactory;
    AgentLifecycleManager agentLifecycleManager;
    AgentResourcesMonitor agentResourcesMonitor;
    AllocationHelperImpl allocationHelper;
    AllocationLifecycleManager allocationLifecycleManager;
    AllocatorService allocatorService;
    BackPopulater backPopulater;
    CatalogService catalogService;
    ContainerEventCreate containerEventCreate;
    EnvironmentResourceManager envResourceManager;
    ImageCredentialLookup imageCredentialLookup;
    InstanceLifecycleManager instanceLifecycleManager;
    K8sLifecycleManager k8sLifecycleManager;
    LoadBalancerService loadBalancerService;
    LoopManager loopManager;
    NetworkLifecycleManager networkLifecycleManager;
    NetworkService networkService;
    PingInstancesMonitor pingInstancesMonitor;
    ProcessProgress progress;
    ProjectTemplateService projectTemplateService;
    ResourceChangeEventListenerImpl resourceChangeEventListener;
    RestartLifecycleManager restartLifecycleManager;
    SecretsLifecycleManager secretsLifecycleManager;
    ServiceLifecycleManager serviceLifecycleManager;
    UpgradeManager upgradeManager;
    VirtualMachineLifecycleManager virtualMachineLifecycleManager;
    VolumeLifecycleManager volumeLifecycleManager;

    List<AnnotatedEventListener> eventListeners = new ArrayList<>();
    List<GenericServiceLauncher> launchers = new ArrayList<>();
    List<InitializationTask> initTasks = new ArrayList<>();

    public Backend(Framework f, Common c, DataAccess d) {
        super();
        this.f = f;
        this.c = c;
        this.d = d;

        setupBackendService();
        addProcessHandlers();
        addTriggers();
        addListeners();
        addTasks();
        addLaunchers();
        addInitializationTasks();

        for (AbstractSampleData data : d.sampleDatas) {
            long start = System.currentTimeMillis();
            data.start();
            CONSOLE_LOG.info("Started {} {}ms", data.getClass().getSimpleName(), (System.currentTimeMillis()-start));
        }

        for (InitializationTask task : initTasks) {
            long start = System.currentTimeMillis();
            task.start();
            CONSOLE_LOG.info("Started {} {}ms", task.getClass().getName(), (System.currentTimeMillis()-start));
        }
    }

    private void setupBackendService() {
        imageCredentialLookup = new DockerImageCredentialLookup(f.jooqConfig);
        progress = new ProcessProgressImpl(f.objectManager, f.eventService);
        allocationHelper = new AllocationHelperImpl(d.instanceDao, f.objectManager, envResourceManager);
        allocatorService = new AllocatorServiceImpl(d.agentDao, c.agentLocator, d.genericMapDao, d.allocatorDao, f.lockManager, f.objectManager,f. processManager, allocationHelper, d.volumeDao, envResourceManager,
                new AccountConstraintsProvider(),
                new BaseConstraintsProvider(d.allocatorDao),
                new AffinityConstraintsProvider(f.jsonMapper, allocationHelper),
                new PortsConstraintProvider(f.objectManager),
                new VolumeAccessModeConstraintProvider(d.allocatorDao, f.objectManager));
        allocationLifecycleManager = new AllocationLifecycleManagerImpl(allocatorService, d.volumeDao, f.objectManager);
        backPopulater = new BackPopulaterImpl(f.jsonMapper, d.volumeDao, f.lockManager, c.dockerTransformer, d.instanceDao, f.objectManager, f.processManager);
        restartLifecycleManager = new RestartLifecycleManagerImpl(backPopulater);
        agentInstanceFactory = new AgentInstanceFactoryImpl(f.objectManager, d.agentDao, d.resourceDao, f.resourceMonitor, f.processManager);
        networkService = new NetworkServiceImpl(d.networkDao, f.jsonMapper, f.resourcePoolManager);
        k8sLifecycleManager = new K8sLifecycleManagerImpl();
        virtualMachineLifecycleManager = new VirtualMachineLifecycleManagerImpl(d.volumeDao, d.storagePoolDao, d.serviceDao, f.jsonMapper, f.objectManager);
        volumeLifecycleManager = new VolumeLifecycleManagerImpl(f.objectManager, f.processManager, d.storagePoolDao, d.volumeDao, f.lockManager);
        networkLifecycleManager = new NetworkLifecycleManagerImpl(f.objectManager, networkService, f.resourcePoolManager);
        agentLifecycleManager = new AgentLifecycleManagerImpl(agentInstanceFactory);
        secretsLifecycleManager = new SecretsLifecycleManagerImpl(c.tokenService, d.storageDriverDao);
        loadBalancerService = new LoadBalancerServiceImpl(f.jsonMapper, f.lockManager, f.objectManager);
        serviceLifecycleManager = new ServiceLifecycleManagerImpl(d.serviceConsumeMapDao, f.objectManager, d.serviceExposeMapDao, f.resourcePoolManager, networkService, d.serviceDao, c.revisionManager, loadBalancerService);
        instanceLifecycleManager = new InstanceLifecycleManagerImpl(k8sLifecycleManager, virtualMachineLifecycleManager, volumeLifecycleManager, f.objectManager, imageCredentialLookup, d.serviceDao, f.transaction, networkLifecycleManager, agentLifecycleManager, backPopulater, restartLifecycleManager, secretsLifecycleManager, allocationLifecycleManager, serviceLifecycleManager, d.instanceDao);
        catalogService = new CatalogServiceImpl(f.jsonMapper, d.resourceDao, f.objectManager, f.processManager);
        projectTemplateService = new ProjectTemplateService(catalogService, f.executorService, f.objectManager, d.resourceDao, f.lockManager);
        upgradeManager = new UpgradeManager(catalogService, d.stackDao, d.resourceDao, f.lockManager, f.processManager);
        agentResourcesMonitor = new AgentResourcesMonitor(d.agentDao, d.storagePoolDao, f.objectManager, f.lockManager, f.eventService);
        pingInstancesMonitor = new PingInstancesMonitorImpl(d.agentDao, f.metaDataManager, c.agentLocator, d.pingInstancesMonitorDao, f.objectManager, containerEventCreate,
                d.containerEventDao);

        Reconcile reconcile = new Reconcile(f, d, c, this);
        loopManager = reconcile.loopManager;
    }

    private void addTriggers() {
        f.triggers.add(new DeploymentUnitReconcileTrigger(loopManager, d.serviceDao, d.volumeDao, f.objectManager));
        f.triggers.add(new ServiceReconcileTrigger(loopManager, f.objectManager));
        f.triggers.add(new MetadataTrigger(envResourceManager));
        f.triggers.add(new SystemStackTrigger(loopManager, f.objectManager));
    }

    private void addProcessHandlers() {
        ProcessRouter r = c.processes;

        AccountProcessManager account = new AccountProcessManager(d.networkDao, d.resourceDao, f.processManager, f.objectManager, d.instanceDao, d.accountDao);
        AgentProcessManager agentProcessManager = new AgentProcessManager(d.accountDao, f.objectManager, f.processManager, c.agentLocator, f.eventService, f.coreSchemaFactory);
        CredentialProcessManager credentialProcessManager = new CredentialProcessManager(f.objectManager, c.transformationService);
        DriverProcessManager driverProcessManager = new DriverProcessManager(f.jsonMapper, f.lockManager, f.objectManager, f.processManager, d.resourceDao, d.storagePoolDao, c.storageService);
        DynamicSchemaProcessManager dynamicSchemaProcessManager = new DynamicSchemaProcessManager(d.dynamicSchemaDao);
        ExternalEventProcessManager externalEventProcessManager = new ExternalEventProcessManager(allocationHelper, d.instanceDao, f.processManager, f.objectManager, d.serviceDao, f.lockManager, f.resourceMonitor, d.resourceDao, f.coreSchemaFactory, d.stackDao);
        HostProcessManager hostProcessManager = new HostProcessManager(d.instanceDao, f.resourceMonitor, f.eventService, d.hostDao, f.metaDataManager, f.objectManager, f.processManager);
        InatorReconcileHandler inatorReconcileHandler = new InatorReconcileHandler(f.objectManager, loopManager);
        InstanceProcessManager instanceProcessManager = new InstanceProcessManager(instanceLifecycleManager, f.processManager);
        NetworkProcessManager networkProcessManager = new NetworkProcessManager(f.objectManager, f.processManager, d.networkDao, f.lockManager, f.jsonMapper, f.resourcePoolManager);
        MountProcessManager mountProcessManager = new MountProcessManager(f.objectManager, f.processManager);
        RegisterProcessManager registerProcessManager = new RegisterProcessManager(d.registerDao, f.resourceMonitor, f.objectManager, f.processManager, d.resourceDao);
        ScheduledUpgradeProcessManager scheduledUpgradeProcessManager = new ScheduledUpgradeProcessManager(catalogService, upgradeManager, f.objectManager, f.processManager);
        ServiceProcessManager serviceProcessManager = new ServiceProcessManager(serviceLifecycleManager, f.objectManager, f.processManager, d.serviceDao);
        StackProcessManager stackProcessManager = new StackProcessManager(f.processManager, f.objectManager);
        SystemStackProcessManager systemStackProcessManager = new SystemStackProcessManager(f.objectManager, f.processManager, loopManager, f.resourceMonitor, d.networkDao);
        VolumeProcessManager volumeProcessManager = new VolumeProcessManager(f.objectManager, f.processManager, d.storagePoolDao, d.genericMapDao);

        AccountLinkRemove accountLinkRemove = new AccountLinkRemove(f.objectManager, f.processManager, f.lockManager, f.eventService);
        ActivateByDefault activateByDefault = new ActivateByDefault(f.objectManager, f.processManager);
        AgentResourceRemove agentResourceRemove = new AgentResourceRemove(f.objectManager, f.processManager);
        containerEventCreate = new ContainerEventCreate(f.objectManager, f.processManager, d.instanceDao, f.lockManager, f.resourceMonitor, c.agentLocator, d.resourceDao);
        ContainerEventPreCreate containerEventPreCreate = new ContainerEventPreCreate(f.objectManager);
        HosttemplateRemove hosttemplateRemove = new HosttemplateRemove(c.secretsService);
        InstanceRemove instanceRemove = new InstanceRemove(c.agentLocator, c.objectSerializerFactory, f.objectManager, f.processManager);
        InstanceStart instanceStart = new InstanceStart(c.agentLocator, c.objectSerializerFactory, f.objectManager, f.processManager);
        InstanceStop instanceStop = new InstanceStop(c.agentLocator, c.objectSerializerFactory, f.objectManager, f.processManager);
        K8sProviderLabels k8sProviderLabels = new K8sProviderLabels(c.agentLocator, c.objectSerializerFactory, f.objectManager, f.processManager, envResourceManager);
        K8sStackCreate k8sStackCreate = new K8sStackCreate(c.agentLocator, c.objectSerializerFactory, f.objectManager, f.processManager);
        K8sStackFinishupgrade k8sStackFinishupgrade = new K8sStackFinishupgrade(c.agentLocator, c.objectSerializerFactory, f.objectManager, f.processManager);
        K8sStackRemove k8sStackRemove = new K8sStackRemove(c.agentLocator, c.objectSerializerFactory, f.objectManager, f.processManager);
        K8sStackRollback k8sStackRollback = new K8sStackRollback(c.agentLocator, c.objectSerializerFactory, f.objectManager, f.processManager);
        K8sStackUpgrade k8sStackUpgrade = new K8sStackUpgrade(c.agentLocator, c.objectSerializerFactory, f.objectManager, f.processManager);
        MachinePreCreate machinePreCreate = new MachinePreCreate();
        PullTaskCreate pullTaskCreate = new PullTaskCreate(allocationHelper, c.agentLocator, progress, imageCredentialLookup, c.objectSerializerFactory);
        MountRemove mountRemove = new MountRemove(c.agentLocator, c.objectSerializerFactory, f.objectManager, f.processManager);
        ProjecttemplateCreate projecttemplateCreate = new ProjecttemplateCreate(loopManager, f.objectManager);
        SecretRemove secretRemove = new SecretRemove(c.secretsService);
        SelectorInstancePostListener selectorInstancePostListener = new SelectorInstancePostListener(serviceLifecycleManager, d.serviceExposeMapDao, f.lockManager, f.objectManager, f.processManager);
        SelectorServiceCreatePostListener selectorServiceCreatePostListener = new SelectorServiceCreatePostListener(serviceLifecycleManager, d.serviceExposeMapDao, f.lockManager, d.serviceConsumeMapDao, f.objectManager, f.processManager);
        ServiceVirtualMachinePreCreate serviceVirtualMachinePreCreate = new ServiceVirtualMachinePreCreate();
        SetRemovedFields setRemovedFields = new SetRemovedFields(f.objectManager);
        StoragePoolRemove storagePoolRemove = new StoragePoolRemove(f.objectManager, f.processManager, d.volumeDao);
        SubnetCreate subnetCreate = new SubnetCreate(f.jsonMapper);
        AgentHostStateUpdate agentHostStateUpdate = new AgentHostStateUpdate(f.coreSchemaFactory, f.processDefinitions, f.eventService, f.objectManager);
        ClearCacheHandler clearCacheHandler = new ClearCacheHandler(f.eventService, d.dbCacheManager);
        ServiceEventCreate serviceEventCreate = new ServiceEventCreate(f.objectManager, loopManager);

        r.handle("account.create", account::preCreate, account::create, registerProcessManager::accountCreate, systemStackProcessManager::accountCreate);
        r.handle("account.remove", account::remove);
        r.handle("account.update", account::update);
        r.handle("account.purge", account::purge);

        r.handle("accountlink.remove", accountLinkRemove);

        r.handle("agent.*", agentHostStateUpdate::postHandle);
        r.handle("agent.activate", agentProcessManager::activate);
        r.handle("agent.reconnect", agentProcessManager::activate);
        r.handle("agent.create", agentProcessManager::create);
        r.handle("agent.remove", agentProcessManager::remove, registerProcessManager::agentRemove);
        r.preHandle("agent.*", agentHostStateUpdate::preHandle);

        r.handle("credential.create", credentialProcessManager::create);

        r.handle("containerevent.create", containerEventPreCreate, containerEventCreate);

        r.handle("deploymentunit.*", inatorReconcileHandler);

        r.handle("dynamicschema.*", clearCacheHandler);
        r.handle("dynamicschema.create", dynamicSchemaProcessManager::create);
        r.handle("dynamicschema.remove", dynamicSchemaProcessManager::remove);

        r.handle("externalevent.create", externalEventProcessManager::preCreate, externalEventProcessManager::create);

        r.handle("genericobject.create", pullTaskCreate, registerProcessManager::genericObjectCreate);

        r.handle("host.create", hostProcessManager::create);
        r.handle("host.provision", hostProcessManager::provision);
        r.handle("host.activate", driverProcessManager::setupPools);
        r.handle("host.remove", hostProcessManager::remove, agentResourceRemove);

        r.handle("hosttemplate.remove", hosttemplateRemove);

        r.handle("instance.create", instanceProcessManager::create);
        r.handle("instance.start", instanceProcessManager::preStart, instanceStart, instanceProcessManager::postStart, k8sProviderLabels, selectorInstancePostListener);
        r.handle("instance.stop", instanceStop, instanceProcessManager::postStop);
        r.handle("instance.restart", instanceProcessManager::restart);
        r.handle("instance.remove", instanceProcessManager::preRemove, instanceRemove);

        r.handle("mount.create", mountProcessManager::create);
        r.handle("mount.deactivate", mountProcessManager::deactivate);
        r.handle("mount.remove", mountRemove);

        r.handle("network.*", networkProcessManager::updateDefaultNetwork);
        r.handle("network.create", networkProcessManager::create);
        r.handle("network.activate", networkProcessManager::activate);
        r.handle("network.remove", networkProcessManager::remove);

        r.handle("networkdriver.activate", networkProcessManager::networkDriverActivate);
        r.handle("networkdriver.remove", networkProcessManager::networkDriverRemove);

        r.handle("physicalhost.create", machinePreCreate);

        r.handle("projecttemplate.create", projecttemplateCreate);

        r.handle("scheduledupgrade.create", scheduledUpgradeProcessManager::create);
        r.handle("scheduledupgrade.start", scheduledUpgradeProcessManager::start);

        r.handle("secret.remove", secretRemove);

        r.handle("service.*", inatorReconcileHandler);
        r.handle("service.create", serviceVirtualMachinePreCreate, driverProcessManager::activate, serviceProcessManager::create, selectorServiceCreatePostListener);
        r.handle("service.update", driverProcessManager::activate, selectorInstancePostListener);
        r.handle("service.activate", driverProcessManager::activate);
        r.handle("service.remove", driverProcessManager::remove, serviceProcessManager::remove);
        r.handle("service.finishupgrade", serviceProcessManager::finishupgrade);

        r.handle("serviceevent.create", serviceEventCreate);

        r.handle("stack.create", k8sStackCreate);
        r.handle("stack.upgrade", k8sStackUpgrade);
        r.handle("stack.rollback", k8sStackRollback);
        r.handle("stack.remove", k8sStackRemove, stackProcessManager::remove, systemStackProcessManager::stackRemove);
        r.handle("stack.finishupgrade", k8sStackFinishupgrade);

        r.handle("storagedriver.activate", driverProcessManager::storageDriverActivate, driverProcessManager::setupPools);
        r.handle("storagedriver.deactivate", driverProcessManager::setupPools);
        r.handle("storagedriver.remove", driverProcessManager::storageDriverRemove);

        r.handle("storagepool.remove", agentResourceRemove, storagePoolRemove);

        r.handle("subnet.create", subnetCreate);

        r.handle("volume.create", volumeProcessManager::create);
        r.handle("volume.update", volumeProcessManager::update);
        r.handle("volume.remove", volumeProcessManager::remove);

        r.handle("*.create", activateByDefault);
        r.handle("*.remove", setRemovedFields);
    }

    private void addListeners() {
        AgentSimulator agentSimulator = new AgentSimulator(f.jsonMapper, f.objectManager, f.resourceMonitor, c.agentLocator, f.eventService,
                new SimulatorFailedProcessor(f.jsonMapper),
                new SimulatorConsoleProcessor(),
                new SimulatorInstanceInspectProcessor(),
                new SimulatorPingProcessor(f.jsonMapper, f.objectManager),
                new SimulatorStartStopProcessor(f.objectManager, f.jsonMapper, f.scheduledExecutorService));
        ProcessEventListener processEventListener = new ProcessEventListenerImpl(f.processServer);
        resourceChangeEventListener = new ResourceChangeEventListenerImpl(f.lockDelegator, f.eventService, f.objectManager, f.jsonMapper);
        TaskManagerEventListener taskManagerEventListener = new TaskManagerEventListenerImpl(c.taskManager, f.lockManager);


        eventListeners.add(agentResourcesMonitor);
        eventListeners.add(agentSimulator);
        eventListeners.add(d.dbCacheManager);
        eventListeners.add(f.resourceMonitor);
        eventListeners.add(new ArchaiusEventListenerImpl());
        eventListeners.add(pingInstancesMonitor);
        eventListeners.add(processEventListener);
        eventListeners.add(resourceChangeEventListener);
        eventListeners.add(taskManagerEventListener);
    }

    private void addTasks() {
        CleanupTaskInstances cleanupTaskInstances = new CleanupTaskInstances(d.taskDao);
        HealthcheckCleanupMonitorImpl healthcheckCleanupMonitorImpl = new HealthcheckCleanupMonitorImpl(f.jooqConfig, f.processManager);
        HostRemoveMonitorImpl hostRemoveMonitorImpl = new HostRemoveMonitorImpl(d.hostDao, d.agentDao, f.processManager);
        PingMonitorImpl pingMonitor = new PingMonitorImpl(agentResourcesMonitor, pingInstancesMonitor, f.processManager, f.objectManager, d.pingDao, c.agentLocator, f.cluster);
        ProcessReplayTask processReplayTask = new ProcessReplayTask(f.processServer);
        PurgeMonitorImpl purgeMonitorImpl = new PurgeMonitorImpl(f.coreSchemaFactory, f.processManager, f.objectManager, f.metaDataManager, f.defaultProcessManager);
        RemoveMonitorImpl removeMonitorImpl = new RemoveMonitorImpl(f.coreSchemaFactory, f.metaDataManager, f.processManager, f.defaultProcessManager, f.objectManager);
        RunScheduledTask runScheduledTask = new RunScheduledTask(upgradeManager);
        TableCleanup tableCleanup = new TableCleanup(f.jooqConfig);
        UpgradeCleanupMonitorImpl upgradeCleanupMonitorImpl = new UpgradeCleanupMonitorImpl(f.jooqConfig, f.processManager, f.jsonMapper);
        UpgradeScheduleTask upgradeScheduleTask = new UpgradeScheduleTask(upgradeManager);

        c.tasks.add(cleanupTaskInstances);
        c.tasks.add(healthcheckCleanupMonitorImpl);
        c.tasks.add(hostRemoveMonitorImpl);
        c.tasks.add(pingMonitor);
        c.tasks.add(processReplayTask);
        c.tasks.add(projectTemplateService);
        c.tasks.add(purgeMonitorImpl);
        c.tasks.add(removeMonitorImpl);
        c.tasks.add(resourceChangeEventListener);
        c.tasks.add(f.resourceMonitor);
        c.tasks.add(runScheduledTask);
        c.tasks.add(tableCleanup);
        c.tasks.add(upgradeCleanupMonitorImpl);
        c.tasks.add(upgradeScheduleTask);
    }

    private void addLaunchers() {
        AuthServiceLauncher authService = new AuthServiceLauncher(f.lockManager, f.lockDelegator, f.scheduledExecutorService, d.accountDao, d.resourceDao, f.resourceMonitor, f.processManager, c.keyProvider, f.objectManager);
        CatalogLauncher catalogLauncher = new CatalogLauncher(f.lockManager, f.lockDelegator, f.scheduledExecutorService, d.accountDao, d.resourceDao, f.resourceMonitor, f.processManager, f.jsonMapper);
        ComposeExecutorLauncher composeExecutorLauncher = new ComposeExecutorLauncher(f.lockManager, f.lockDelegator, f.scheduledExecutorService, d.accountDao, d.resourceDao, f.resourceMonitor, f.processManager, f.cluster);
        MachineLauncher machineLauncher = new MachineLauncher(f.lockManager, f.lockDelegator, f.scheduledExecutorService, d.accountDao, d.resourceDao, f.resourceMonitor, f.processManager, f.cluster);
        SecretsApiLauncher secretsApiLauncher = new SecretsApiLauncher(f.lockManager, f.lockDelegator, f.scheduledExecutorService, d.accountDao, d.resourceDao, f.resourceMonitor, f.processManager, d.dataDao);
        TelemetryLauncher telemetryLauncher = new TelemetryLauncher(f.lockManager, f.lockDelegator, f.scheduledExecutorService, d.accountDao, d.resourceDao, f.resourceMonitor, f.processManager, f.cluster);
        WebhookServiceLauncher webhookServiceLauncher = new WebhookServiceLauncher(f.lockManager, f.lockDelegator, f.scheduledExecutorService, d.accountDao, d.resourceDao, f.resourceMonitor, f.processManager, c.keyProvider);
        WebsocketProxyLauncher websocketProxyLauncher = new WebsocketProxyLauncher(f.lockManager, f.lockDelegator, f.scheduledExecutorService, d.accountDao, d.resourceDao, f.resourceMonitor, f.processManager, f.cluster);

        launchers.add(authService);
        launchers.add(catalogLauncher);
        launchers.add(composeExecutorLauncher);
        launchers.add(machineLauncher);
        launchers.add(secretsApiLauncher);
        launchers.add(c.serviceAccountCreateStartup);
        launchers.add(telemetryLauncher);
        launchers.add(webhookServiceLauncher);
        launchers.add(websocketProxyLauncher);
    }

    private void addInitializationTasks() {
        AnnotatedListenerRegistration annotatedListenerRegistration = new AnnotatedListenerRegistration(eventListeners, f.eventService, f.jsonMapper, f.lockManager);
        MachineDriverLoader machineDriverLoader = new MachineDriverLoader(f.lockManager, f.objectManager, f.processManager, f.jsonMapper);

        initTasks.add(annotatedListenerRegistration);
        initTasks.add(f.lockDelegator);
        initTasks.add(machineDriverLoader);
        initTasks.add(projectTemplateService);
        initTasks.add(c.serviceAccountCreateStartup);
        initTasks.add(c.taskManager);
    }

}
