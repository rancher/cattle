package io.cattle.platform.app.components;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.manager.impl.LoopManagerImpl;
import io.cattle.platform.inator.Deployinator;
import io.cattle.platform.inator.factory.InatorFactoryinator;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.impl.DeployinatorImpl;
import io.cattle.platform.loop.factory.LoopFactoryImpl;

public class Reconcile {

    Framework f;
    DataAccess d;
    Common c;
    Backend b;
    LoopManager loopManager;

    public Reconcile(Framework f, DataAccess d, Common c, Backend b) {
        super();
        this.f = f;
        this.d = d;
        this.c = c;
        this.b = b;
        init();
    }

    protected void init() {
        InatorServices inatorServices = new InatorServices();

        inatorServices.objectManager = f.objectManager;
        inatorServices.serviceDao = d.serviceDao;
        inatorServices.resourceDao = d.resourceDao;
        inatorServices.processManager = f.processManager;
        inatorServices.metadataManager = f.metaDataManager;
        inatorServices.jsonMapper = f.jsonMapper;
        inatorServices.networkService = b.networkService;
        inatorServices.idFormatter = f.idFormatter;
        inatorServices.lockManager = f.lockManager;
        inatorServices.allocationHelper = b.allocationHelper;
        inatorServices.poolManager = f.resourcePoolManager;
        inatorServices.instanceDao = d.instanceDao;
        inatorServices.hostDao = d.hostDao;
        inatorServices.dataDao = d.dataDao;
        inatorServices.envResourceManager = b.envResourceManager;

        InatorFactoryinator inatorFactoryinator = new InatorFactoryinator(inatorServices);
        ActivityService activityService = new ActivityService(f.objectManager, f.eventService);
        Deployinator deployinator = new DeployinatorImpl(inatorFactoryinator, f.objectManager, f.lockManager, activityService, b.serviceLifecycleManager);
        LoopFactory loopFactory = new LoopFactoryImpl(activityService, b.catalogService, deployinator, b.envResourceManager, f.eventService, d.hostDao, b.loopManager, f.objectManager, f.processManager, f.scheduledExecutorService, b.serviceLifecycleManager);
        loopManager = new LoopManagerImpl(loopFactory, f.executorService, f.objectManager, f.scheduledExecutorService);

        inatorServices.loopManager = loopManager;
    }

}
