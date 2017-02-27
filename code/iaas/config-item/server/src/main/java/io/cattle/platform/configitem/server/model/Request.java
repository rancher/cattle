package io.cattle.platform.configitem.server.model;

import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.ItemVersion;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface Request {

    public static final int OK = 200;
    public static final int NOT_FOUND = 404;

    ItemVersion getAppliedVersion();

    ItemVersion getCurrentVersion();

    String getItemName();

    Client getClient();

    void setResponseCode(int code);

    void setContentType(String contentType);

    void setContentEncoding(String contentEncoding);

    OutputStream getOutputStream() throws IOException;

    Map<String, Object> getParams();

}
