package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.util.type.InitializationTask;

import java.util.List;
import java.util.Map;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class CatalogLauncher extends GenericServiceLauncher implements InitializationTask {

    private static final DynamicStringProperty CATALOG_URL = ArchaiusUtil.getString("catalog.url");
    private static final DynamicStringProperty CATALOG_REFRESH_INTERVAL = ArchaiusUtil.getString("catalog.refresh.interval.seconds");
    private static final DynamicStringProperty CATALOG_BINARY = ArchaiusUtil.getString("catalog.service.executable");
    private static final DynamicBooleanProperty LAUNCH_CATALOG = ArchaiusUtil.getBoolean("catalog.execute");

    @Override
    protected boolean shouldRun() {
        return LAUNCH_CATALOG.get();
    }

    @Override
    protected String binaryPath() {
        return CATALOG_BINARY.get();
    }

    @Override
    protected DynamicStringProperty getReloadSetting() {
        return CATALOG_URL;
    }

    @Override
    protected void prepareProcess(ProcessBuilder pb) {
        List<String> args = pb.command();
        args.add("-catalogUrl");
        args.add(CATALOG_URL.get());
        args.add("-refreshInterval");
        args.add(CATALOG_REFRESH_INTERVAL.get());
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
    }

    @Override
    protected LockDefinition getLock() {
        return null;
    }

    @Override
    protected boolean isReady() {
        return true;
    }

}
