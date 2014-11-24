package io.cattle.platform.configitem.server.model.util;

import io.cattle.platform.configitem.server.model.impl.GenericConfigItemFactory;
import io.cattle.platform.configitem.server.resource.AbstractCachingResourceRoot;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigItemResourceUtil {

    private static final Logger log = LoggerFactory.getLogger(ConfigItemResourceUtil.class);

    public static Map<String,Map<String,URL>> processUrlRoot(boolean ignoreNotFound, String root, URL[] resources) throws IOException {
        List<URL> baseUrls = Collections.list(GenericConfigItemFactory.class.getClassLoader().getResources(root));
        if ( baseUrls.size() == 0 ) {
            if ( ignoreNotFound ) {
                return new HashMap<String, Map<String,URL>>();
            } else {
                throw new IllegalStateException("Failed to find [" + root + "]");
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

        return config;
    }

    protected static String trimLeading(String text) {
        if ( text.startsWith("/") || text.startsWith("\\") ) {
            return text.substring(1);
        } else {
            return text;
        }
    }
}
