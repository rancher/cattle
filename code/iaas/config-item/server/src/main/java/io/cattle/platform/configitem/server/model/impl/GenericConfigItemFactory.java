package io.cattle.platform.configitem.server.model.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.context.ConfigItemContextFactory;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.ConfigItemFactory;
import io.cattle.platform.configitem.server.resource.AbstractCachingResourceRoot;
import io.cattle.platform.configitem.server.resource.FileBasedResourceRoot;
import io.cattle.platform.configitem.server.resource.URLBaseResourceRoot;
import io.cattle.platform.configitem.server.template.TemplateFactory;
import io.cattle.platform.configitem.server.template.TemplatesBasedArchiveItem;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.util.type.Named;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class GenericConfigItemFactory implements ConfigItemFactory, Named  {

    private static final Logger log = LoggerFactory.getLogger(GenericConfigItemFactory.class);

    private static final DynamicBooleanProperty IGNORE_FS = ArchaiusUtil.getBoolean("config.item.ignore.filesystem");

    ConfigItemStatusManager versionManager;
    List<ConfigItem> items = new ArrayList<ConfigItem>();
    List<ConfigItemContextFactory> factories;
    TemplateFactory templateFactory;
    String root;
    String devRelativePath;
    URL[] resources;
    String name;
    boolean ignoreNotFound = false;

    protected File findDevRoot() {
        if ( IGNORE_FS.get() ) {
            return null;
        }

        URL url = GenericConfigItemFactory.class.getClassLoader().getResource(root);
        if ( url != null && "file".equals(url.getProtocol()) ) {
            File scriptsRoot = new File(url.getPath(), devRelativePath);
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
            File devRoot = findDevRoot();
            if ( devRoot == null ) {
                log.info("Using classpath for location [{}]", name);
                processUrlRoot();
            } else {
                log.info("Using filesytem for location [{}] at [{}]", name, devRoot);
                processFileRoot(devRoot);
            }
    }

    protected void processUrlRoot() throws IOException {
        List<URL> baseUrls = Collections.list(GenericConfigItemFactory.class.getClassLoader().getResources(root));
        if ( baseUrls.size() == 0 ) {
            if ( ignoreNotFound ) {
                return;
            } else {
                throw new IllegalStateException("Failed to find [" + root + "] for config items [" + name + "]");
            }
        }

        Map<String,Map<String,URL>> config = new TreeMap<String, Map<String,URL>>();
        outer:
        for ( URL resource : resources ) {
            String name = null;
            String path = null;

            for ( URL baseUrl : baseUrls ) {
                String base = baseUrl.toExternalForm();
                String fullUrl = resource.toExternalForm();
                if ( ! fullUrl.startsWith(base) ) {
                    continue;
                }

                String part = trimLeading(fullUrl.substring(base.length()));
                int idx = StringUtils.indexOfAny(part, "/", "\\");
                if ( idx != -1 ) {
                    name = part.substring(0, idx);
                    path = part.substring(idx);
                    break;
                }
            }

            if ( name == null ) {
                log.error("Ignoring resource [{}] can not find it realtive to root [{}]", resource, root);
                continue;
            }

            path = trimLeading(path);

            if ( StringUtils.isBlank(path) || path.endsWith("/") || path.endsWith("\\") ) {
                continue;
            }

            for ( String part : path.split("[/\\\\]") ) {
                if ( AbstractCachingResourceRoot.shouldIgnore(part) ) {
                    continue outer;
                }
            }

            Map<String,URL> urlMapping = config.get(name);
            if ( urlMapping == null ) {
                urlMapping = new TreeMap<String, URL>();
                config.put(name, urlMapping);
            }

            urlMapping.put(path, resource);
        }

        for ( Map.Entry<String, Map<String,URL>> entry : config.entrySet() ) {
            String item = entry.getKey();

            URLBaseResourceRoot resourceRoot = new URLBaseResourceRoot(entry.getValue());
            resourceRoot.scan();
            log.info("Adding item [{}] resources [{}]", item, entry.getValue().size());
            items.add(new TemplatesBasedArchiveItem(item, versionManager, resourceRoot, templateFactory, getFactories(item)));
        }
    }

    protected List<ConfigItemContextFactory> getFactories(String item) {
        List<ConfigItemContextFactory> result = new ArrayList<ConfigItemContextFactory>();

        for ( ConfigItemContextFactory factory : factories ) {
            for ( String check : factory.getItems() ) {
                if ( item.equals(check) ) {
                    result.add(factory);
                }
            }
        }

        return result;
    }

    protected String trimLeading(String text) {
        if ( text.startsWith("/") || text.startsWith("\\") ) {
            return text.substring(1);
        } else {
            return text;
        }
    }

    protected void processFileRoot(File root) throws IOException {
        String[] children = root.list();

        if ( children == null )
            return;

        for ( String child : children ) {
            File childFile = new File(root, child);
            if ( ! child.startsWith(".") && childFile.isDirectory() ) {
                FileBasedResourceRoot itemResource = new FileBasedResourceRoot(childFile);
                itemResource.scan();
                items.add(new TemplatesBasedArchiveItem(child, versionManager, itemResource, templateFactory, getFactories(child)));
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

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getDevRelativePath() {
        return devRelativePath;
    }

    public void setDevRelativePath(String devRelativePath) {
        this.devRelativePath = devRelativePath;
    }

    public URL[] getResources() {
        return resources;
    }

    public void setResources(URL[] resources) {
        this.resources = resources;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIgnoreNotFound() {
        return ignoreNotFound;
    }

    public void setIgnoreNotFound(boolean ignoreNotFound) {
        this.ignoreNotFound = ignoreNotFound;
    }

    public List<ConfigItemContextFactory> getFactories() {
        return factories;
    }

    @Inject
    public void setFactories(List<ConfigItemContextFactory> factories) {
        this.factories = factories;
    }

}
