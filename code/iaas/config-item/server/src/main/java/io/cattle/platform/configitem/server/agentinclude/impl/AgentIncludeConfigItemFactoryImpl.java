package io.cattle.platform.configitem.server.agentinclude.impl;

import io.cattle.platform.configitem.context.ConfigItemContextFactory;
import io.cattle.platform.configitem.server.agentinclude.AgentIncludeMap;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.ConfigItemFactory;
import io.cattle.platform.configitem.server.model.util.ConfigItemResourceUtil;
import io.cattle.platform.configitem.server.resource.FileBasedResourceRoot;
import io.cattle.platform.configitem.server.resource.ResourceRoot;
import io.cattle.platform.configitem.server.resource.URLBaseResourceRoot;
import io.cattle.platform.configitem.server.template.TemplateFactory;
import io.cattle.platform.configitem.server.template.TemplatesBasedArchiveItem;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class AgentIncludeConfigItemFactoryImpl implements ConfigItemFactory {

    String root;
    URL[] resources;
    String fileRoot;
    AgentIncludeMap map;
    TemplateFactory templateFactory;
    ConfigItemStatusManager versionManager;

    @Override
    public Collection<ConfigItem> getConfigItems() throws IOException {
        Set<String> itemSet = new HashSet<>();
        List<ConfigItem> itemList = new ArrayList<>();

        for ( String name : map.getNamedMaps() ) {
            itemList.add(new AgentPackagesConfigItem(name, versionManager, getResourceRoot(), templateFactory, map));

            Map<String,String> values = map.getMap(name);
            for ( String key : values.keySet() ) {
                String value = values.get(key);

                if ( value != null && ! itemSet.contains(value) ) {
                    if ( AgentPackagesConfigItem.isDevVersion(value) ) {
                        FileBasedResourceRoot itemResource = new FileBasedResourceRoot(new File(value));
                        itemResource.scan();

                        TemplatesBasedArchiveItem archiveItem = new TemplatesBasedArchiveItem(key, versionManager, itemResource, templateFactory, new ArrayList<ConfigItemContextFactory>());
                        archiveItem.setDynamicallyApplied(true);
                        itemList.add(archiveItem);
                    }

                    itemSet.add(value);
                }
            }
        }

        return itemList;
    }

    protected ResourceRoot getResourceRoot() throws IOException {
        if ( new File(fileRoot).exists() ) {
            FileBasedResourceRoot root = new FileBasedResourceRoot(new File(fileRoot));
            root.scan();
            return root;
        } else {
            Map<String,Map<String, URL>> configs = ConfigItemResourceUtil.processUrlRoot(false, root, resources);
            if ( configs.size() == 0 ) {
                throw new IllegalStateException("Failed to find to find agent-include");
            }

            URLBaseResourceRoot root = new URLBaseResourceRoot(configs.values().iterator().next());
            root.scan();

            return root;
        }
    }

    public AgentIncludeMap getMap() {
        return map;
    }

    @Inject
    public void setMap(AgentIncludeMap map) {
        this.map = map;
    }

    public TemplateFactory getTemplateFactory() {
        return templateFactory;
    }

    @Inject
    public void setTemplateFactory(TemplateFactory templateFactory) {
        this.templateFactory = templateFactory;
    }

    public ConfigItemStatusManager getVersionManager() {
        return versionManager;
    }

    @Inject
    public void setVersionManager(ConfigItemStatusManager versionManager) {
        this.versionManager = versionManager;
    }

    public String getFileRoot() {
        return fileRoot;
    }

    @Inject
    public void setFileRoot(String fileRoot) {
        this.fileRoot = fileRoot;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public URL[] getResources() {
        return resources;
    }

    public void setResources(URL[] resources) {
        this.resources = resources;
    }

}
