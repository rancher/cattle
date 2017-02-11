package io.cattle.platform.app;

import io.cattle.platform.datasource.DataSourceFactory;
import io.cattle.platform.datasource.JMXDataSourceFactoryImpl;
import io.cattle.platform.db.jooq.logging.LoggerListener;
import io.cattle.platform.extension.impl.ExtensionManagerImpl;
import io.cattle.platform.json.JacksonJsonMapper;
import io.cattle.platform.liquibase.JarInJarServiceLocator;
import io.cattle.platform.liquibase.Loader;
import io.cattle.platform.spring.resource.SpringConfigurableExecutorService;
import io.cattle.platform.spring.resource.SpringResourceLoader;
import io.cattle.platform.util.concurrent.NamedExecutorService;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import javax.management.MalformedObjectNameException;
import javax.sql.DataSource;

import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.tools.StopWatchListener;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.scheduling.concurrent.ScheduledExecutorFactoryBean;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@Configuration
public class SystemConfig {

    @Bean
    JMXDataSourceFactoryImpl DataSourceFactory() {
        return new JMXDataSourceFactoryImpl();
    }

    @Bean
    @DependsOn("ArchaiusStartup")
    DataSource DataSource(DataSourceFactory factory) {
        return factory.createDataSource("cattle");
    }

    @Bean
    Loader liquibaseCore(DataSourceFactory factory) {
        JarInJarServiceLocator locator = new JarInJarServiceLocator();
        locator.init();

        Loader loader = new Loader();
        loader.setDataSourceFactory(factory);
        loader.setChangeLog("classpath:db/changelog.xml");
        return loader;
    }

    @Bean
    DataSourceConnectionProvider JooqConnectionProvider(@Qualifier("DataSource") DataSource ds) {
        return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(ds));
    }

    @Bean
    @Primary
    io.cattle.platform.db.jooq.config.Configuration JooqConfiguration(DataSourceConnectionProvider dscp) {
        LoggerListener logger = new LoggerListener();
        logger.setMaxLength(1000);

        io.cattle.platform.db.jooq.config.Configuration config = new io.cattle.platform.db.jooq.config.Configuration();
        config.setName("cattle");
        config.setConnectionProvider(dscp);
        config.setListeners(Arrays.asList(
                logger,
                new StopWatchListener()));

        return config;
    }

    @Bean
    io.cattle.platform.db.jooq.config.Configuration LockingJooqConfiguration(DataSourceConnectionProvider dscp) {
        Settings settings = new Settings();
        settings.setExecuteWithOptimisticLocking(true);

        LoggerListener logger = new LoggerListener();
        logger.setMaxLength(1000);

        io.cattle.platform.db.jooq.config.Configuration config = new io.cattle.platform.db.jooq.config.Configuration();
        config.setName("cattle");
        config.setConnectionProvider(dscp);
        config.setListeners(Arrays.asList(
                logger,
                new StopWatchListener()));
        config.setSettings(settings);

        return config;
    }

    @Bean
    DataSourceTransactionManager CoreTransactionManager(@Qualifier("DataSource") DataSource ds) {
        DataSourceTransactionManager dstm = new DataSourceTransactionManager();
        dstm.setDataSource(ds);
        return dstm;
    }

    @Bean
    SpringResourceLoader ResourceLoader() {
        return new SpringResourceLoader();
    }

    @Bean(autowire = Autowire.BY_TYPE)
    JacksonJsonMapper CoreJsonMapper() {
        return new JacksonJsonMapper();
    }

    @Bean
    SimpleModule NoOpJacksonModule() {
        return new SimpleModule();
    }

    @Bean
    NamedExecutorService process(ExtensionManagerImpl em,  @Qualifier("ProcessEventExecutorService") ExecutorService es) {
        NamedExecutorService nes = new NamedExecutorService();
        nes.setName("process");
        nes.setExecutorService(es);
        return nes;
    }

    @Bean
    NamedExecutorService blockingprocess(ExtensionManagerImpl em,  @Qualifier("ProcessBlockingExecutorService") ExecutorService es) {
        NamedExecutorService nes = new NamedExecutorService();
        nes.setName("blockingprocess");
        nes.setExecutorService(es);
        return nes;
    }

    @Bean
    ListeningExecutorService CoreListeningExecutorService(@Qualifier("CoreExecutorService") ExecutorService es) {
        return MoreExecutors.listeningDecorator(es);
    }

    @Bean
    ScheduledExecutorFactoryBean CoreScheduledExecutorService() {
        ScheduledExecutorFactoryBean sefb = new ScheduledExecutorFactoryBean();
        sefb.setPoolSize(10);
        return sefb;
    }

    @Bean
    SpringConfigurableExecutorService ProcessEventExecutorService() throws MalformedObjectNameException {
        return SpringConfigurableExecutorService.byName("ProcessEventExecutorService");
    }

    @Bean
    SpringConfigurableExecutorService ProcessNonBlockingExecutorService() throws MalformedObjectNameException {
        return SpringConfigurableExecutorService.byName("ProcessNonBlockingExecutorService", new ThreadPoolExecutor.DiscardPolicy());
    }

    @Bean
    SpringConfigurableExecutorService ProcessBlockingExecutorService() throws MalformedObjectNameException {
        return SpringConfigurableExecutorService.byName("ProcessBlockingExecutorService", new ThreadPoolExecutor.DiscardPolicy());
    }

    @Bean
    SpringConfigurableExecutorService CoreExecutorService() throws MalformedObjectNameException {
        return SpringConfigurableExecutorService.byName("CoreExecutorService");
    }

    @Bean
    SpringConfigurableExecutorService EventExecutorService() throws MalformedObjectNameException {
        /*
         * This is for very short lived tasks, no blocking
         */
        return SpringConfigurableExecutorService.byName("EventExecutorService");
    }
}