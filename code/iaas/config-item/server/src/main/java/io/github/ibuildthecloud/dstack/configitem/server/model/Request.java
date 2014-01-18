package io.github.ibuildthecloud.dstack.configitem.server.model;

import io.github.ibuildthecloud.dstack.configitem.model.Client;
import io.github.ibuildthecloud.dstack.configitem.model.ItemVersion;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface Request {

    public static final int OK = 200;
    public static final int NOT_FOUND = 404;

    ItemVersion getAppliedVersion();

    String getItemName();

    Client getClient();

    void setResponseCode(int code);

    void setContentType(String contentType);

    OutputStream getOutputStream() throws IOException;

    Map<String, Object> getParams();

}
