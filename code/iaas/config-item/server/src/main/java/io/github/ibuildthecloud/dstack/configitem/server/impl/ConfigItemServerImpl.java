package io.github.ibuildthecloud.dstack.configitem.server.impl;

import io.github.ibuildthecloud.dstack.configitem.model.ItemVersion;
import io.github.ibuildthecloud.dstack.configitem.registry.ConfigItemRegistry;
import io.github.ibuildthecloud.dstack.configitem.server.model.ConfigItem;
import io.github.ibuildthecloud.dstack.configitem.server.model.Request;
import io.github.ibuildthecloud.dstack.configitem.server.service.ConfigItemServer;
import io.github.ibuildthecloud.dstack.configitem.version.ConfigItemStatusManager;
import io.github.ibuildthecloud.dstack.lock.LockCallbackNoReturn;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigItemServerImpl implements ConfigItemServer, InitializationTask {

    private static final Logger log = LoggerFactory.getLogger(ConfigItemServerImpl.class);

    ConfigItemStatusManager versionManager;
    ConfigItemRegistry itemRegistry;
    LockManager lockManager;

    @Override
    public void handleRequest(Request req) throws IOException {
        ItemVersion version = req.getAppliedVersion();

        if (version == null) {
            handleDownload(req);
        } else {
            handleApplied(req);
        }
    }

    protected void handleApplied(Request req) {
        ItemVersion version = req.getAppliedVersion();

        if ( ! versionManager.isAssigned(req.getClient(), req.getItemName()) ) {
            log.error("Client [{}] is reporting applied on non-assigned item [{}]", req.getClient(), req.getItemName());
            req.setResponseCode(Request.NOT_FOUND);
            return;
        }

        if (version.isLatest()) {
            ConfigItem item = itemRegistry.getConfigItem(req.getItemName());
            if (item == null) {
                req.setResponseCode(Request.NOT_FOUND);
                return;
            }
            log.info("Setting item [{}] to latest for [{}]", req.getItemName(), req.getClient());
            versionManager.setLatest(req.getClient(), req.getItemName(), item.getSourceRevision());
        } else {
            log.info("Setting item [{}] to version [{}] for [{}]", req.getItemName(), req.getAppliedVersion(),
                    req.getClient());
            versionManager.setApplied(req.getClient(), req.getItemName(), req.getAppliedVersion());
        }
    }

    protected void handleDownload(Request req) throws IOException {
        ConfigItem item = itemRegistry.getConfigItem(req.getItemName());

        if (item == null) {
            log.info("Client [{}] requested unknown item [{}]", req.getClient(), req.getItemName());
            req.setResponseCode(Request.NOT_FOUND);
            return;
        }

        if ( ! versionManager.isAssigned(req.getClient(), req.getItemName()) ) {
            log.error("Client [{}] requesting non-assigned item [{}]", req.getClient(), req.getItemName());
            req.setResponseCode(Request.NOT_FOUND);
            return;
        }

        log.info("Processing [{}] for client [{}]", req.getItemName(), req.getClient());
        item.handleRequest(req);
    }

    @Override
    public void start() {
        syncSourceVersion();
    }

    @Override
    public void stop() {
    }

    protected void syncSourceVersion() {
        lockManager.lock(new SyncSourceVersionLock(), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                for ( ConfigItem item : itemRegistry.getConfigItems() ) {
                    versionManager.setItemSourceVersion(item.getName(), item.getSourceRevision());
                }
            }
        });
    }


    public ConfigItemStatusManager getVersionManager() {
        return versionManager;
    }

    @Inject
    public void setVersionManager(ConfigItemStatusManager versionManager) {
        this.versionManager = versionManager;
    }

    public ConfigItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    @Inject
    public void setItemRegistry(ConfigItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

}
