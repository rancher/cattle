package io.cattle.platform.docker.machine.launch;

import org.apache.http.client.fluent.Request;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.util.type.InitializationTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class CatalogLauncher extends GenericServiceLauncher implements InitializationTask {

    private static final DynamicStringProperty CATALOG_URL = ArchaiusUtil.getString("catalog.url");
    private static final DynamicStringProperty CATALOG_REFRESH_INTERVAL = ArchaiusUtil.getString("catalog.refresh.interval.seconds");
    private static final DynamicStringProperty CATALOG_BINARY = ArchaiusUtil.getString("catalog.service.executable");
    private static final DynamicBooleanProperty LAUNCH_CATALOG = ArchaiusUtil.getBoolean("catalog.execute");

    public static class CatalogEntry {
        String name;
        String repoURL;
        String branch;

        public CatalogEntry() {

        }

        public String getRepoURL() {
            return repoURL;
        }


        public void setRepoURL(String repoURL) {
            this.repoURL = repoURL;
        }


        public String getBranch() {
            return branch;
        }


        public void setBranch(String branch) {
            this.branch = branch;
        }


        public void setName(String name) {
            this.name = name;
        }


        public String getName() {
            return name;
        }
    }

    public static class ConfigFileFields {
        List<CatalogEntry> Catalogs;

        public List<CatalogEntry> getCatalogs() {
            return Catalogs;
        }

        public void setCatalogs(List<CatalogEntry> catalogs) {
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
        args.add("-configFile");
        prepareConfigFile();
        args.add("repo.json");
        args.add("-refreshInterval");
        args.add(CATALOG_REFRESH_INTERVAL.get());
    }

    protected void prepareConfigFile() throws IOException {
        File configFile = new File("repo.json");

        String catUrl = CATALOG_URL.get();
        ConfigFileFields configCatalogEntries = new ConfigFileFields();
        FileOutputStream fos;
        if(catUrl.startsWith("{")) {
            configCatalogEntries = jsonMapper.readValue(catUrl, ConfigFileFields.class);
            fos = new FileOutputStream(configFile.getAbsoluteFile());
            jsonMapper.writeValue(fos, configCatalogEntries);
        }
        else {
            String []catalogs = catUrl.split(",");
            List<CatalogEntry> catalogEntryList = new ArrayList<>();
            for (String catalog: catalogs) {
                CatalogEntry entry = new CatalogEntry();
                String[]splitted = catalog.split("=");
                entry.setName(splitted[0]);
                entry.setRepoURL(splitted[1]);
                entry.setBranch("master");
                catalogEntryList.add(entry);
            }
            configCatalogEntries.setCatalogs(catalogEntryList);
            fos = new FileOutputStream(configFile.getAbsoluteFile());
            jsonMapper.writeValue(fos, configCatalogEntries);
        }
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

    public void reload() {
        try {
            prepareConfigFile();
            Request.Post("http://localhost:8088/v1-catalog/templates?action=refresh").execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
