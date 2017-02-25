package org.apache.cloudstack.configitem.server.model.impl;

import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.server.model.impl.AbstractRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TestRequest extends AbstractRequest {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public TestRequest() {
        setItemName("testitem");
        setClient(new Client(String.class, 123));
    }

    public String getResponseContent() throws IOException {
        return new String(baos.toByteArray(), "UTF-8");
    }

    @Override
    public OutputStream getOutputStream() {
        return baos;
    }

    @Override
    public void setContentEncoding(String contentEncoding) {
    }
}
