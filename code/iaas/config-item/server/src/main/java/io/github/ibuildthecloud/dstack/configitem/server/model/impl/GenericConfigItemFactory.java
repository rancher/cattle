package io.github.ibuildthecloud.dstack.configitem.server.model.impl;

import io.github.ibuildthecloud.dstack.configitem.server.model.ConfigItem;
import io.github.ibuildthecloud.dstack.configitem.server.model.ConfigItemFactory;
import io.github.ibuildthecloud.dstack.configitem.server.resource.FileBasedResourceRoot;
import io.github.ibuildthecloud.dstack.configitem.server.template.TemplateFactory;
import io.github.ibuildthecloud.dstack.configitem.server.template.TemplatesBasedArchiveItem;
import io.github.ibuildthecloud.dstack.configitem.version.ConfigItemStatusManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericConfigItemFactory implements ConfigItemFactory  {

    private static final Logger log = LoggerFactory.getLogger(GenericConfigItemFactory.class);

//    private static final DynamicStringProperty LOCATIONS = ArchaiusUtil.getString("config.item.locations");

    ConfigItemStatusManager versionManager;
    List<ConfigItem> items = new ArrayList<ConfigItem>();
    TemplateFactory templateFactory;

    protected File findDevRoot() {
        URL url = GenericConfigItemFactory.class.getResource("/systemvm/.systemvmscripts");
        if ( url != null && "file".equals(url.getProtocol()) ) {
            File scriptsRoot = new File(new File(url.getPath()).getParentFile(), "../../../src/main/resources/systemvm");
            if ( scriptsRoot.exists() ) {
                try {
                    return new File(scriptsRoot.getCanonicalPath());
                } catch (IOException e) {
                    log.error("Failed to get canonical path of [{}]", scriptsRoot, e);
                }
            }
        }

        return null;
    }

    protected void locateResourceRoot() throws IOException {
//        try {
            File f = findDevRoot();
            processFileRoot(f, false);
            System.err.println("File: " + f);
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    protected void processFileRoot(File root, boolean useCache) throws IOException {
        String[] children = root.list();

        if ( children == null )
            return;

        for ( String child : children ) {
            File childFile = new File(root, child);
            if ( ! child.startsWith(".") && childFile.isDirectory() ) {
                FileBasedResourceRoot itemResource = new FileBasedResourceRoot(childFile, useCache);
                itemResource.scan();
                items.add(new TemplatesBasedArchiveItem(child, versionManager, itemResource, templateFactory));
            }
        }
    }

    @Override
    public Collection<ConfigItem> getConfigItems() throws IOException {
        locateResourceRoot();
        return items;
    }

    public ConfigItemStatusManager getVersionManager() {
        return versionManager;
    }

    @Inject
    public void setVersionManager(ConfigItemStatusManager versionManager) {
        this.versionManager = versionManager;
    }

    public TemplateFactory getTemplateFactory() {
        return templateFactory;
    }

    @Inject
    public void setTemplateFactory(TemplateFactory templateFactory) {
        this.templateFactory = templateFactory;
    }

}
