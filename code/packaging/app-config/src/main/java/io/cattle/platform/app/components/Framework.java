package io.cattle.platform.app.components;

import io.cattle.platform.db.jooq.logging.LoggerListener;
import io.cattle.platform.engine.manager.impl.ProcessRecordDao;
import io.cattle.platform.engine.manager.impl.jooq.JooqProcessRecordDao;
import io.cattle.platform.extension.ExtensionManager;
import io.cattle.platform.extension.dynamic.DynamicExtensionManager;
import io.cattle.platform.iaas.api.object.postinit.AccountFieldPostInitHandler;
import io.cattle.platform.json.JacksonJsonMapper;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.defaults.JsonDefaultsProvider;
import io.cattle.platform.object.impl.JooqObjectManager;
import io.cattle.platform.object.impl.TransactionDelegateImpl;
import io.cattle.platform.object.meta.impl.DefaultObjectMetaDataManager;
import io.cattle.platform.object.postinit.ObjectDataPostInstantiationHandler;
import io.cattle.platform.object.postinit.ObjectDefaultsPostInstantiationHandler;
import io.cattle.platform.object.postinit.ObjectPostInstantiationHandler;
import io.cattle.platform.object.postinit.SpecialFieldsPostInstantiationHandler;
import io.cattle.platform.object.postinit.UUIDPostInstantiationHandler;
import io.cattle.platform.schema.processor.AuthSchemaAdditionsPostProcessor;
import io.cattle.platform.schema.processor.JpaWritablePostProcessor;
import io.cattle.platform.schema.processor.JsonFileOverlayPostProcessor;
import io.cattle.platform.schema.processor.StripSuffixPostProcessor;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaFactoryImpl;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.json.JacksonMapper;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.jooq.Configuration;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultTransactionProvider;
import org.jooq.tools.StopWatchListener;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class Framework {

    DataSource dataSource;
    Configuration jooqConfig;
    Configuration lockingJooqConfig;
    Configuration newConnJooqConfig;
    JsonMapper jsonMapper;
    SchemaFactoryImpl coreSchemaFactory = new SchemaFactoryImpl();
    DefaultObjectMetaDataManager metaDataManager = new DefaultObjectMetaDataManager(coreSchemaFactory);
    io.github.ibuildthecloud.gdapi.json.JsonMapper schemaJsonMapper = new JacksonMapper();
    JooqObjectManager objectManager;
    TransactionDelegate transaction;
    ProcessRecordDao processRecordDao;
    JsonDefaultsProvider jsonDefaultsProvider;
    ObjectDefaultsPostInstantiationHandler objectDefaultsPostInstantiationHandler;
    ExtensionManager extensionManager = new DynamicExtensionManager();

    public Framework() throws IOException {
        Bootstrap bootstrap = new Bootstrap();
        this.dataSource = bootstrap.dataSource;
        this.jooqConfig = bootstrap.jooqConfig;
        init();
    }

    public void start() {
        jsonDefaultsProvider.start();
        objectDefaultsPostInstantiationHandler.start();
        metaDataManager.start();
    }

    private void init() {
        setupDb();
        setupJson();
        setupObjectFramework();
    }

    private void setupObjectFramework() {
        jsonDefaultsProvider = new JsonDefaultsProvider(coreSchemaFactory, jsonMapper, "schema/defaults", "schema/defaults/overrides");
        objectDefaultsPostInstantiationHandler = new ObjectDefaultsPostInstantiationHandler(jsonDefaultsProvider);

        List<SchemaPostProcessor> postProcessors = coreSchemaFactory.getPostProcessors();
        postProcessors.add(new StripSuffixPostProcessor("Record"));
        postProcessors.add(new JpaWritablePostProcessor());
        postProcessors.add(new JsonFileOverlayPostProcessor(null, jsonMapper, schemaJsonMapper, "schema/base"));
        postProcessors.add(new AuthSchemaAdditionsPostProcessor());

        objectManager = new JooqObjectManager(coreSchemaFactory, metaDataManager, jooqConfig, lockingJooqConfig, transaction);
        List<ObjectPostInstantiationHandler> postInitHandlers = objectManager.getPostInitHandlers();
        postInitHandlers.add(objectDefaultsPostInstantiationHandler);
        postInitHandlers.add(new SpecialFieldsPostInstantiationHandler(coreSchemaFactory));
        postInitHandlers.add(new AccountFieldPostInitHandler());
        postInitHandlers.add(new ObjectDataPostInstantiationHandler(jsonMapper));
        postInitHandlers.add(new UUIDPostInstantiationHandler());

        processRecordDao = new JooqProcessRecordDao(jsonMapper, objectManager, metaDataManager);
    }

    private void setupJson() {
        JacksonJsonMapper mapper = new JacksonJsonMapper();
        mapper.setModules(Arrays.asList(new SimpleModule()));
        mapper.init();
        this.jsonMapper = mapper;
    }

    private void setupDb() {
        transaction = new TransactionDelegateImpl(jooqConfig);

        LoggerListener logger = new LoggerListener();
        logger.setMaxLength(1000);

        setupLockingJooq(logger);
        setupNewConnJooq(logger);
    }

    private void setupLockingJooq(LoggerListener logger) {
        io.cattle.platform.db.jooq.config.Configuration config = new io.cattle.platform.db.jooq.config.Configuration();
        config.setName("cattle");
        config.setConnectionProvider(jooqConfig.connectionProvider());
        config.setTransactionProvider(jooqConfig.transactionProvider());
        config.setListeners(Arrays.asList(
                logger,
                new StopWatchListener()));
        config.setSettings(new Settings().withExecuteWithOptimisticLocking(true));
        this.lockingJooqConfig = config;
    }

    private void setupNewConnJooq(LoggerListener logger) {
        io.cattle.platform.db.jooq.config.Configuration config = new io.cattle.platform.db.jooq.config.Configuration();
        config.setName("cattle");
        config.setConnectionProvider(jooqConfig.connectionProvider());
        config.setTransactionProvider(new DefaultTransactionProvider(jooqConfig.connectionProvider(), false));
        config.setListeners(Arrays.asList(
                logger,
                new StopWatchListener()));
        this.newConnJooqConfig = config;
    }

}
