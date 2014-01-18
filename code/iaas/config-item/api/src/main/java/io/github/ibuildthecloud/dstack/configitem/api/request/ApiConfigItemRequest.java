package io.github.ibuildthecloud.dstack.configitem.api.request;

import io.github.ibuildthecloud.dstack.configitem.model.ItemVersion;
import io.github.ibuildthecloud.dstack.configitem.model.impl.DefaultClient;
import io.github.ibuildthecloud.dstack.configitem.server.model.impl.AbstractRequest;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.io.OutputStream;

public class ApiConfigItemRequest extends AbstractRequest {

    ApiRequest request;

    public ApiConfigItemRequest(String id, long agentId, ItemVersion itemVersion, ApiRequest request) {
        super(id, new DefaultClient(Agent.class, agentId), itemVersion, request.getRequestParams());
        this.request = request;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        request.setResponseContentType("application/octet-stream");
        return request.getOutputStream();
    }

}
