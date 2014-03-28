package io.cattle.platform.configitem.server.template.impl;

import io.cattle.platform.configitem.server.resource.Resource;
import io.cattle.platform.configitem.server.template.Template;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;

public class RawResourceTemplate implements Template {

    Resource resource;

    public RawResourceTemplate(Resource resource) {
        super();
        this.resource = resource;
    }

    @Override
    public String getOutputName() {
        return resource.getName();
    }

    @Override
    public void execute(Map<String, Object> context, OutputStream os) throws IOException {
        InputStream is = null;
        try {
            is = resource.getInputStream();
            IOUtils.copy(is, os);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Override
    public long getSize() {
        return resource.getSize();
    }

}
