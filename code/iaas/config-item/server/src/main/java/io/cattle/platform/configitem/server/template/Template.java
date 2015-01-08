package io.cattle.platform.configitem.server.template;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface Template {

    String getOutputName();

    long getSize();

    void execute(Map<String, Object> context, OutputStream os) throws IOException;

}
