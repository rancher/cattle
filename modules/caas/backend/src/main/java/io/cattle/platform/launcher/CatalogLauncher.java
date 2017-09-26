package io.cattle.platform.launcher;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class CatalogLauncher extends GenericServiceLauncher {

    private static final DynamicStringProperty CATALOG_URL = ArchaiusUtil.getString("catalog.url");
    private static final DynamicStringProperty CATALOG_REFRESH_INTERVAL = ArchaiusUtil.getString("catalog.refresh.interval.seconds");
    private static final DynamicStringProperty CATALOG_BINARY = ArchaiusUtil.getString("catalog.service.executable");
    public static final DynamicStringProperty DB_PARAMS = ArchaiusUtil.getString("db.cattle.go.params");
    private static final DynamicBooleanProperty LAUNCH_CATALOG = ArchaiusUtil.getBoolean("catalog.execute");
    private static final DynamicStringProperty RANCHER_SERVER_VERSION = ArchaiusUtil.getString("rancher.server.version");

    public static class CatalogEntry {
        String url;
        String branch;
        String kind;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public CatalogEntry() {
            this.branch = "master";
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }
    }

    public static class ConfigFileFields {
        Map<String, CatalogEntry> Catalogs;

        public Map<String, CatalogEntry> getCatalogs() {
            return Catalogs;
        }

        public void setCatalogs(Map<String, CatalogEntry> catalogs) {
            Catalogs = catalogs;
        }

        public ConfigFileFields() {
        }
    }

    JsonMapper jsonMapper;

    public CatalogLauncher(LockManager lockManager, LockDelegator lockDelegator, ScheduledExecutorService executor, AccountDao accountDao,
            GenericResourceDao resourceDao, ResourceMonitor resourceMonitor, ObjectProcessManager processManager, JsonMapper jsonMapper) {
        super(lockManager, lockDelegator, executor, accountDao, resourceDao, resourceMonitor, processManager);
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldRun() {
        return LAUNCH_CATALOG.get();
    }

    @Override
    protected String binaryPath() {
        return CATALOG_BINARY.get();
    }

    @Override
    protected List<DynamicStringProperty> getReloadSettings() {
        List<DynamicStringProperty> list = new ArrayList<>();
        list.add(CATALOG_URL);
        list.add(CATALOG_REFRESH_INTERVAL);
        list.add(RANCHER_SERVER_VERSION);
        return list;
    }

    @Override
    protected void prepareProcess(ProcessBuilder pb) throws IOException {
        List<String> args = pb.command();
        args.add("--config");
        prepareConfigFile();
        args.add("repo.json");
        args.add("--refresh-interval");
        args.add(CATALOG_REFRESH_INTERVAL.get());
    }

    protected void prepareConfigFile() throws IOException {
        File configFile = new File("repo.json");

        try (OutputStream os = new FileOutputStream(configFile.getAbsoluteFile())) {
            prepareConfigFile(os, jsonMapper);
        }
    }

    public static void prepareConfigFile(OutputStream fos, JsonMapper jsonMapper) throws IOException {
        String catUrl = CATALOG_URL.get();
        ConfigFileFields configCatalogEntries = new ConfigFileFields();
        if (catUrl.startsWith("{")) {
            configCatalogEntries = jsonMapper.readValue(catUrl, ConfigFileFields.class);
        } else {
            String[] catalogs = catUrl.split(",");
            Map<String, CatalogEntry> catalogEntryMap = new HashMap<>();
            for (String catalog : catalogs) {
                CatalogEntry entry = new CatalogEntry();
                String[] splitted = catalog.split("=");
                entry.setUrl(splitted[1]);
                entry.setBranch("master");
                catalogEntryMap.put(splitted[0], entry);
            }
            configCatalogEntries.setCatalogs(catalogEntryMap);
        }

        setReleaseBranch(configCatalogEntries);
        jsonMapper.writeValue(fos, configCatalogEntries);
    }

    protected static void setReleaseBranch(ConfigFileFields fields) {
        String branch = RANCHER_SERVER_VERSION.get();
        if (StringUtils.isBlank(branch)) {
            branch = "master";
        } else {
            String[] parts = branch.split("[.]");
            if (parts.length > 2) {
                branch = String.format("%s.%s-release", parts[0], parts[1]);
            }
        }

        for (CatalogEntry entry : fields.getCatalogs().values()) {
            if ("${RELEASE}".equals(entry.getBranch())) {
                entry.setBranch(branch);
            }
        }
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
        env.put("CATALOG_SERVICE_MYSQL_ADDRESS", String.format("%s:%s", DBSettings.DB_HOST.get(),
                DBSettings.DB_PORT.get()));
        env.put("CATALOG_SERVICE_MYSQL_DBNAME", DBSettings.DB_NAME.get());
        env.put("CATALOG_SERVICE_MYSQL_USER", DBSettings.DB_USER.get());
        env.put("CATALOG_SERVICE_MYSQL_PASSWORD", DBSettings.DB_PASS.get());
        env.put("CATALOG_SERVICE_MYSQL_PARAMS", DB_PARAMS.get() == null ? "" : DB_PARAMS.get());
    }

    @Override
    protected LockDefinition getLock() {
        return null;
    }

    @Override
    public void reload() {
        if (!shouldRun()) {
            return;
        }

        try {
            prepareConfigFile();
            Request.Post("http://localhost:8088/v1-catalog/templates?action=refresh")
                .addHeader(ProjectConstants.PROJECT_HEADER, "global").execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
