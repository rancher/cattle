package io.cattle.platform.app.components;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.allocator.service.AllocationHelperImpl;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.manager.impl.LoopManagerImpl;
import io.cattle.platform.inator.Deployinator;
import io.cattle.platform.inator.factory.InatorFactoryinator;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.impl.DeployinatorImpl;
import io.cattle.platform.loop.factory.LoopFactoryImpl;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.metadata.impl.EnvironmentResourceManagerImpl;
import io.cattle.platform.metadata.impl.MetadataObjectFactory;

public class Reconcile {

    Framework f;
    DataAccess d;
    Common c;
    Backend b;
    LoopManager loopManager;
    MetadataManager metadataManager;
    InatorServices inatorServices = new InatorServices();
    AllocationHelperImpl allocationHelper;


    public Reconcile(Framework f, DataAccess d, Common c, Backend b) {
        super();
        this.f = f;
        this.d = d;
        this.c = c;
        this.b = b;
        init();
    }

    protected void init() {
        inatorServices.allocationHelper = b.allocationHelper;
        inatorServices.dataDao = d.dataDao;
        inatorServices.hostDao = d.hostDao;
        inatorServices.idFormatter = f.idFormatter;
        inatorServices.instanceDao = d.instanceDao;
        inatorServices.jsonMapper = f.jsonMapper;
        inatorServices.lockManager = f.lockManager;
        inatorServices.networkService = b.networkService;
        inatorServices.objectManager = f.objectManager;
        inatorServices.objectMetadataManager = f.metaDataManager;
        inatorServices.poolManager = f.resourcePoolManager;
        inatorServices.processManager = f.processManager;
        inatorServices.resourceDao = d.resourceDao;
        inatorServices.serviceDao = d.serviceDao;
        inatorServices.scheduledExecutorService = f.scheduledExecutorService;

        InatorFactoryinator inatorFactoryinator = new InatorFactoryinator(inatorServices);
        ActivityService activityService = new ActivityService(f.objectManager, f.eventService);
        Deployinator deployinator = new DeployinatorImpl(inatorFactoryinator, f.objectManager, f.lockManager, activityService, b.serviceLifecycleManager);
        LoopFactoryImpl loopFactory = new LoopFactoryImpl(activityService, c.catalogService, deployinator, f.eventService, d.hostDao, f.objectManager, f.processManager, f.scheduledExecutorService, b.serviceLifecycleManager, null, null, c.agentLocator, c.objectSerializer);
        loopManager = new LoopManagerImpl(loopFactory, f.executorService, f.objectManager, f.scheduledExecutorService);
        metadataManager = new EnvironmentResourceManagerImpl(new MetadataObjectFactory(f.objectManager), loopManager, f.lockManager, f.objectManager, f.eventService, d.clusterDao, f.triggers);

        loopFactory.setEnvResourceManager(metadataManager);
        loopFactory.setLoopManager(loopManager);

        inatorServices.allocationHelper = allocationHelper = new AllocationHelperImpl(d.instanceDao, f.objectManager, metadataManager);
        inatorServices.loopManager = loopManager;
        inatorServices.metadataManager = metadataManager;
    }

}
