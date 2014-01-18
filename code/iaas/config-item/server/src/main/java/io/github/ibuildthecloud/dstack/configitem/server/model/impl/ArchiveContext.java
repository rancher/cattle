package io.github.ibuildthecloud.dstack.configitem.server.model.impl;

import io.github.ibuildthecloud.dstack.configitem.server.model.Request;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;


public class ArchiveContext {

    String version;
    Request request;
    TarArchiveOutputStream taos;
    Map<String, Object> data = new HashMap<String, Object>();

    public ArchiveContext(Request request, TarArchiveOutputStream taos, String version) {
        super();
        this.request = request;
        this.taos = taos;
        this.version = version;

        data.put("version", version);
    }

    public Request getRequest() {
        return request;
    }

    public TarArchiveOutputStream getOutputStream() {
        return taos;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getVersion() {
        return version;
    }

}
