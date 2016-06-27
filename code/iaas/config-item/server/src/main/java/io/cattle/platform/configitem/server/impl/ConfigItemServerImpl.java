package io.cattle.platform.configitem.server.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.registry.ConfigItemRegistry;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.RefreshableConfigItem;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.server.service.ConfigItemServer;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.util.type.InitializationTask;

import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringListProperty;

public class ConfigItemServerImpl implements ConfigItemServer, InitializationTask {

    private static final DynamicStringListProperty PRELOAD = ArchaiusUtil.getList("config.item.name.preload");

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

    protected ConfigItem retrieveItem(Request req) {
        String arch = RequestUtils.makeSingularStringIfCan(req.getParams().get("arch"));
        String archSuffix = "";
        if (arch != null && arch.length() != 0 && !arch.equals("amd64")) {
            archSuffix = "-" + arch;
        }
        ConfigItem item = itemRegistry.getConfigItem(req.getItemName() + archSuffix);
        if (item == null) {
            item = itemRegistry.getConfigItem(req.getItemName());
        }
        return item;
    }

    protected void handleApplied(Request req) {
        ItemVersion version = req.getAppliedVersion();
        ConfigItem item = retrieveItem(req);

        if (!versionManager.isAssigned(req.getClient(), req.getItemName())) {
            log.error("Client [{}] is reporting applied on non-assigned item [{}]", req.getClient(), req.getItemName());
            req.setResponseCode(Request.NOT_FOUND);
            return;
        }

        if (version.isLatest()) {
            if (item == null) {
                req.setResponseCode(Request.NOT_FOUND);
                return;
            }
            log.info("Setting item [{}] to latest for [{}]", req.getItemName(), req.getClient());
            versionManager.setLatest(req.getClient(), req.getItemName(), item.getSourceRevision());
        } else {
            if (item.getSourceRevision().equals(version.getSourceRevision())) {
                log.info("Setting item [{}] to version [{}] for [{}]", req.getItemName(), req.getAppliedVersion(), req.getClient());
                versionManager.setApplied(req.getClient(), req.getItemName(), req.getAppliedVersion());
            } else {
                log.info("Ignoring item [{}] to version [{}] for [{}] because source revision [{}] does not match expect [{}]", req.getItemName(), req
                        .getAppliedVersion(), req.getClient(), version.getSourceRevision(), item.getSourceRevision());
            }
        }
    }

    protected void handleDownload(Request req) throws IOException {
        ConfigItem item = retrieveItem(req);

        if (item == null) {
            log.info("Client [{}] requested unknown item [{}]", req.getClient(), req.getItemName());
            req.setResponseCode(Request.NOT_FOUND);
            return;
        }

        if (!versionManager.isAssigned(req.getClient(), req.getItemName())) {
            if (item.isDynamicallyApplied()) {
                ConfigUpdateRequest updateRequest = new ConfigUpdateRequest(req.getClient()).withDeferredTrigger(true);
                updateRequest.addItem(req.getItemName());
                versionManager.updateConfig(updateRequest);
            } else {
                log.error("Client [{}] requesting non-assigned item [{}]", req.getClient(), req.getItemName());
                req.setResponseCode(Request.NOT_FOUND);
                return;
            }
        }

        log.info("Processing [{}] for client [{}]", req.getItemName(), req.getClient());
        item.handleRequest(req);
    }

    @Override
    public void start() {
        syncSourceVersion(true);
    }

    @Override
    public void stop() {
    }

    @Override
    public void syncSourceVersion() {
        syncSourceVersion(false);
    }

    protected void syncSourceVersion(final boolean initial) {
        lockManager.lock(new SyncSourceVersionLock(), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                for (ConfigItem item : itemRegistry.getConfigItems()) {
                    if (!initial) {
                        if (item instanceof RefreshableConfigItem) {
                            try {
                                ((RefreshableConfigItem) item).refresh();
                            } catch (IOException e) {
                                log.error("Failed to refresh item [{}]", item.getName(), e);
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                    versionManager.setItemSourceVersion(item.getName(), item.getSourceRevision());
                }

                for (String item : PRELOAD.get()) {
                    versionManager.setItemSourceVersion(item, "");
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
