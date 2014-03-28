package io.cattle.platform.configitem.api.request;

import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.model.impl.DefaultClient;
import io.cattle.platform.configitem.server.model.impl.AbstractRequest;
import io.cattle.platform.core.model.Agent;
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
        return request.getOutputStream();
    }

    @Override
    public void setContentType(String contentType) {
        request.setResponseContentType(contentType);
    }

}
