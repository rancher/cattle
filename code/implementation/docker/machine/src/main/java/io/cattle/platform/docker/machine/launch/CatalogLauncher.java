package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.iaas.api.manager.HaConfigManager;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.util.type.InitializationTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.http.client.fluent.Request;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class CatalogLauncher extends GenericServiceLauncher implements InitializationTask {

    private static final DynamicStringProperty CATALOG_URL = ArchaiusUtil.getString("catalog.url");
    private static final DynamicStringProperty CATALOG_REFRESH_INTERVAL = ArchaiusUtil.getString("catalog.refresh.interval.seconds");
    private static final DynamicStringProperty CATALOG_BINARY = ArchaiusUtil.getString("catalog.service.executable");
    private static final DynamicStringProperty DB_PARAMS = ArchaiusUtil.getString("db.cattle.go.params");
    private static final DynamicBooleanProperty LAUNCH_CATALOG = ArchaiusUtil.getBoolean("catalog.execute");

    public static class CatalogEntry {
        String url;
        String branch;

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

    @Inject
    JsonMapper jsonMapper;

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
        List<DynamicStringProperty> list = new ArrayList<DynamicStringProperty>();
        list.add(CATALOG_URL);
        list.add(CATALOG_REFRESH_INTERVAL);
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

        String catUrl = CATALOG_URL.get();
        ConfigFileFields configCatalogEntries = new ConfigFileFields();
        FileOutputStream fos = null;
        try {
            if (catUrl.startsWith("{")) {
                configCatalogEntries = jsonMapper.readValue(catUrl, ConfigFileFields.class);
                fos = new FileOutputStream(configFile.getAbsoluteFile());
                jsonMapper.writeValue(fos, configCatalogEntries);
            }
            else {
                String[] catalogs = catUrl.split(",");
                Map<String, CatalogEntry> catalogEntryMap = new HashMap<String, CatalogEntry>();
                for (String catalog : catalogs) {
                    CatalogEntry entry = new CatalogEntry();
                    String[] splitted = catalog.split("=");
                    entry.setUrl(splitted[1]);
                    entry.setBranch("master");
                    catalogEntryMap.put(splitted[0], entry);
                }
                configCatalogEntries.setCatalogs(catalogEntryMap);
                fos = new FileOutputStream(configFile.getAbsoluteFile());
                jsonMapper.writeValue(fos, configCatalogEntries);
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
        env.put("CATALOG_SERVICE_MYSQL_ADDRESS", String.format("%s:%s", HaConfigManager.DB_HOST.get(),
                HaConfigManager.DB_PORT.get()));
        env.put("CATALOG_SERVICE_MYSQL_DBNAME", HaConfigManager.DB_NAME.get());
        env.put("CATALOG_SERVICE_MYSQL_USER", HaConfigManager.DB_USER.get());
        env.put("CATALOG_SERVICE_MYSQL_PASSWORD", HaConfigManager.DB_PASS.get());
        env.put("CATALOG_SERVICE_MYSQL_PARAMS", DB_PARAMS.get() == null ? "" : DB_PARAMS.get());
    }

    @Override
    protected LockDefinition getLock() {
        return null;
    }

    @Override
    protected boolean isReady() {
        return true;
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
