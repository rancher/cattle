package io.cattle.platform.configitem.server.resource;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBasedResourceRoot extends AbstractCachingResourceRoot implements ResourceRoot {

    private static final Logger log = LoggerFactory.getLogger(FileBasedResourceRoot.class);

    File base;
    boolean useCache;

    public FileBasedResourceRoot(File base) {
        super();
        this.base = base;
    }

    @Override
    public synchronized Collection<Resource> getResources() throws IOException {
        scan();
        return super.getResources();
    }

    @Override
    protected Collection<Resource> scanResources() throws IOException {
        Map<String, Resource> result = new TreeMap<String, Resource>();
        scan("", base, result);
        return result.values();
    }

    protected void scan(String path, File current, Map<String, Resource> result) {
        String[] children = current.list();
        if ( children == null )
            return;

        for ( String child : children ) {
            if ( shouldIgnore(child) ) {
                continue;
            }

            File childFile = new File(current,child);
            String childName = new File(path, child).getPath();

            if ( childFile.isDirectory() ) {
                scan(childName, childFile, result);
            } else {
                if ( childFile.canRead() ) {
                    result.put(childName, new FileResource(childName, childFile));
                } else {
                    log.warn("Can not read [{}], ignoring", childFile.getAbsolutePath());
                }
            }
        }
    }
}
