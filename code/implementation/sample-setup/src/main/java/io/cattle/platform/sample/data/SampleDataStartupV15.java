package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.SettingTable.*;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.docker.machine.launch.CatalogLauncher;
import io.cattle.platform.docker.machine.launch.CatalogLauncher.CatalogEntry;
import io.cattle.platform.docker.machine.launch.CatalogLauncher.ConfigFileFields;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;


public class SampleDataStartupV15 extends AbstractSampleData {

    private static List<String> OLD_URL = Arrays.asList("https://git.rancher.io/rancher-catalog.git",
            "https://github.com/rancher/rancher-catalog.git");

    @Inject
    JsonMapper jsonMapper;
    @Inject
    ObjectManager objectManager;

    @Override
    protected String getName() {
        return "sampleDataVersion15";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        Setting setting = objectManager.findAny(Setting.class, SETTING.NAME, "catalog.url");
        if (setting == null) {
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            CatalogLauncher.prepareConfigFile(baos, jsonMapper);
            ConfigFileFields files = jsonMapper.readValue(baos.toByteArray(), ConfigFileFields.class);
            boolean changed = false;
            if (files.getCatalogs() != null && files.getCatalogs().containsKey("library")) {
                CatalogEntry ce = files.getCatalogs().get("library");
                if (OLD_URL.contains(ce.getUrl()) && "master".equals(ce.getBranch())) {
                    changed = true;
                    ce.setBranch("${RELEASE}");
                }
            }

            if (changed) {
                baos.reset();
                jsonMapper.writeValue(baos, files);
                setting.setValue(baos.toString());
                objectManager.persist(setting);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}