package io.cattle.platform.app.components;

import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.async.retry.impl.RetryTimeoutServiceImpl;
import io.cattle.platform.db.jooq.logging.LoggerListener;
import io.cattle.platform.engine.manager.impl.DefaultProcessManager;
import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.manager.impl.jooq.JooqProcessRecordDao;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.impl.ProcessHandlerRegistryImpl;
import io.cattle.platform.engine.server.ProcessInstanceExecutor;
import io.cattle.platform.engine.server.ProcessServer;
import io.cattle.platform.engine.server.impl.ProcessInstanceDispatcherImpl;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.memory.InMemoryEventService;
import io.cattle.platform.hazelcast.membership.ClusterService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.impl.LockDelegatorImpl;
import io.cattle.platform.lock.impl.LockManagerImpl;
import io.cattle.platform.lock.provider.impl.InMemoryLockProvider;
import io.cattle.platform.object.defaults.JsonDefaultsProvider;
import io.cattle.platform.object.impl.JooqObjectManager;
import io.cattle.platform.object.impl.TransactionDelegateImpl;
import io.cattle.platform.object.meta.impl.DefaultObjectMetaDataManager;
import io.cattle.platform.object.monitor.impl.ResourceMonitorImpl;
import io.cattle.platform.object.postinit.AccountFieldPostInitHandler;
import io.cattle.platform.object.postinit.ObjectDataPostInstantiationHandler;
import io.cattle.platform.object.postinit.ObjectDefaultsPostInstantiationHandler;
import io.cattle.platform.object.postinit.ObjectPostInstantiationHandler;
import io.cattle.platform.object.postinit.SetPropertiesPostInstantiationHandler;
import io.cattle.platform.object.postinit.SpecialFieldsPostInstantiationHandler;
import io.cattle.platform.object.postinit.UUIDPostInstantiationHandler;
import io.cattle.platform.object.process.impl.DefaultObjectProcessManager;
import io.cattle.platform.object.process.impl.ObjectExecutionExceptionHandler;
import io.cattle.platform.process.monitor.EventNotificationChangeMonitor;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.impl.ResourcePoolManagerImpl;
import io.cattle.platform.resource.pool.mac.MacAddressGeneratorFactory;
import io.cattle.platform.resource.pool.mac.MacAddressPrefixGeneratorFactory;
import io.cattle.platform.resource.pool.port.EnvironmentPortGeneratorFactory;
import io.cattle.platform.resource.pool.port.HostPortGeneratorFactory;
import io.cattle.platform.resource.pool.subnet.SubnetAddressGeneratorFactory;
import io.cattle.platform.schema.processor.AuthSchemaAdditionsPostProcessor;
import io.cattle.platform.schema.processor.JpaWritablePostProcessor;
import io.cattle.platform.schema.processor.JsonFileOverlayPostProcessor;
import io.cattle.platform.schema.processor.StripSuffixPostProcessor;
import io.cattle.platform.util.resource.ClassloaderResourceLoader;
import io.cattle.platform.util.resource.ResourceLoader;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaFactoryImpl;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.id.TypeIdFormatter;
import io.github.ibuildthecloud.gdapi.json.JacksonMapper;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.jooq.Configuration;
import org.jooq.conf.SettingsTools;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultTransactionProvider;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Framework {

    private static AtomicInteger COUNTER = new AtomicInteger();

    SchemaFactoryImpl coreSchemaFactory = new SchemaFactoryImpl();
    Map<String, ProcessDefinition> processDefinitions = new HashMap<>();
    List<Trigger> triggers = new ArrayList<>();

    ClusterService cluster;
    Configuration jooqConfig;
    Configuration lockingJooqConfig;
    Configuration newConnJooqConfig;
    DataSource dataSource;
    DefaultObjectMetaDataManager metaDataManager = new DefaultObjectMetaDataManager(coreSchemaFactory);
    DefaultObjectProcessManager processManager;
    DefaultProcessManager defaultProcessManager;
    ProcessHandlerRegistryImpl processRegistry = new ProcessHandlerRegistryImpl(processDefinitions);
    ProcessInstanceExecutor processInstanceExecutor;
    ProcessServer processServer;
    IdFormatter idFormatter;
    io.github.ibuildthecloud.gdapi.json.JsonMapper schemaJsonMapper = new JacksonMapper();
    JooqObjectManager objectManager;
    JsonDefaultsProvider jsonDefaultsProvider;
    JsonMapper jsonMapper;
    ObjectDefaultsPostInstantiationHandler objectDefaultsPostInstantiationHandler;
    ProcessRecordDao processRecordDao;
    ResourceLoader resourceLoader = new ClassloaderResourceLoader();
    ResourceMonitorImpl resourceMonitor;
    ResourcePoolManager resourcePoolManager;
    RetryTimeoutService retryTimeoutService;
    TransactionDelegate transaction;

    ThreadPoolExecutor executorService = new ThreadPoolExecutor(0, 2000, 5L, TimeUnit.SECONDS, new SynchronousQueue<>(), (r) -> {
        Thread t = new Thread(r);
        t.setName("core-" + COUNTER.incrementAndGet());
        return t;
    });
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2, (r) -> {
        Thread t = new Thread(r);
        t.setName("sched-core-" + COUNTER.incrementAndGet());
        return t;
    });

    LockManager lockManager;
    LockDelegatorImpl lockDelegator;
    EventService eventService;

    public Framework(Bootstrap bootstrap) throws IOException {
        this.dataSource = bootstrap.dataSource;
        this.jooqConfig = bootstrap.jooqConfig;
        this.jsonMapper = bootstrap.jsonMapper;
        this.cluster = bootstrap.cluster;
        init();
    }

    public void wireUpTypes() {
        metaDataManager.start();
        jsonDefaultsProvider.start();
        objectDefaultsPostInstantiationHandler.start();
    }

    private void init() {
        setupLockAndEventing();
        setupDb();
        setupObjectFramework();
        setupServices();
    }

    private void setupLockAndEventing() {
        retryTimeoutService = new RetryTimeoutServiceImpl(executorService, scheduledExecutorService);
        lockManager = new LockManagerImpl(new InMemoryLockProvider());
        lockDelegator = new LockDelegatorImpl(lockManager, executorService);
        eventService = new InMemoryEventService(retryTimeoutService, executorService, jsonMapper);
    }

    private void setupServices() {
        processRecordDao = new JooqProcessRecordDao(jooqConfig, jsonMapper, objectManager, metaDataManager);
        defaultProcessManager = new DefaultProcessManager(processRecordDao,
                lockManager,
                eventService,
                new ObjectExecutionExceptionHandler(),
                new EventNotificationChangeMonitor(objectManager),
                processDefinitions,
                triggers);
        processInstanceExecutor = new ProcessInstanceDispatcherImpl(defaultProcessManager);
        processManager = new DefaultObjectProcessManager(defaultProcessManager, coreSchemaFactory, objectManager);
        idFormatter = buildIdFormatter();
        resourceMonitor = new ResourceMonitorImpl(objectManager, metaDataManager, idFormatter);
        resourcePoolManager = new ResourcePoolManagerImpl(objectManager,
                new MacAddressGeneratorFactory(),
                new MacAddressPrefixGeneratorFactory(),
                new SubnetAddressGeneratorFactory(),
                new EnvironmentPortGeneratorFactory(),
                new HostPortGeneratorFactory());
        processServer = new ProcessServer(scheduledExecutorService, executorService, executorService, processInstanceExecutor, defaultProcessManager, cluster, triggers);
    }

    private void setupObjectFramework() {
        jsonDefaultsProvider = new JsonDefaultsProvider(coreSchemaFactory, jsonMapper, "schema/defaults", "schema/defaults/overrides");
        objectDefaultsPostInstantiationHandler = new ObjectDefaultsPostInstantiationHandler(jsonDefaultsProvider);

        List<SchemaPostProcessor> postProcessors = coreSchemaFactory.getPostProcessors();
        postProcessors.add(new StripSuffixPostProcessor("Record"));
        postProcessors.add(new JpaWritablePostProcessor());
        postProcessors.add(new JsonFileOverlayPostProcessor(resourceLoader, jsonMapper, schemaJsonMapper, "schema/base"));
        postProcessors.add(new AuthSchemaAdditionsPostProcessor());

        objectManager = new JooqObjectManager(coreSchemaFactory, metaDataManager, jooqConfig, lockingJooqConfig, transaction);

        List<ObjectPostInstantiationHandler> postInitHandlers = objectManager.getPostInitHandlers();
        postInitHandlers.add(objectDefaultsPostInstantiationHandler);
        postInitHandlers.add(new SpecialFieldsPostInstantiationHandler(coreSchemaFactory));
        postInitHandlers.add(new SetPropertiesPostInstantiationHandler());
        postInitHandlers.add(new AccountFieldPostInitHandler());
        postInitHandlers.add(new ObjectDataPostInstantiationHandler(jsonMapper));
        postInitHandlers.add(new UUIDPostInstantiationHandler());

    }

    private void setupDb() {
        transaction = new TransactionDelegateImpl(jooqConfig);

        LoggerListener logger = new LoggerListener();
        logger.setMaxLength(1000);

        setupLockingJooq(logger);
        setupNewConnJooq(logger);
    }

    private void setupLockingJooq(LoggerListener logger) {
        this.lockingJooqConfig = new DefaultConfiguration()
                .set(jooqConfig.dialect())
                .set(SettingsTools.clone(jooqConfig.settings()).withExecuteWithOptimisticLocking(true))
                .set(jooqConfig.connectionProvider())
                .set(jooqConfig.transactionProvider())
                .set(jooqConfig.executeListenerProviders());
    }

    private void setupNewConnJooq(LoggerListener logger) {
        this.newConnJooqConfig = new DefaultConfiguration()
                .set(jooqConfig.dialect())
                .set(SettingsTools.clone(jooqConfig.settings()).withExecuteWithOptimisticLocking(true))
                .set(jooqConfig.connectionProvider())
                .set(new DefaultTransactionProvider(jooqConfig.connectionProvider(), false))
                .set(jooqConfig.executeListenerProviders());
    }

    private IdFormatter buildIdFormatter() {
        TypeIdFormatter idFormatter = new TypeIdFormatter(coreSchemaFactory);
        Map<String, String> mapping = new HashMap<>();
        mapping.put("stack", "st");
        mapping.put("secret", "se");

        idFormatter.setSchemaFactory(coreSchemaFactory);
        idFormatter.setPlainTypes(new HashSet<>(Arrays.asList("typeDocumentation", "scripts", "identity")));
        idFormatter.setTypeMappings(mapping);

        return idFormatter;
    }

}
