package io.github.ibuildthecloud.dstack.configitem.server.resource;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class FileBasedResourceRoot extends AbstractCachingResourceRoot implements ResourceRoot {

    File base;
    boolean useCache;

    public FileBasedResourceRoot(File base, boolean useCache) {
        super();
        this.base = base;
        this.useCache = useCache;
    }

    @Override
    public synchronized Collection<Resource> getResources() throws IOException {
        if ( ! useCache ) {
            scan();
        }
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
            if ( child.startsWith(".") )
                continue;

            File childFile = new File(current,child);
            String childName = new File(path, child).getPath();

            if ( childFile.isDirectory() ) {
                scan(childName, childFile, result);
            } else {
                result.put(childName, new FileResource(childName, childFile));
            }
        }
    }
}
