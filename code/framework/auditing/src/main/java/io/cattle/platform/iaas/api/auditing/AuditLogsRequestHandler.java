package io.cattle.platform.iaas.api.auditing;

import io.cattle.platform.api.auth.Policy;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;

import java.io.IOException;

import javax.inject.Inject;

public class AuditLogsRequestHandler extends AbstractApiRequestHandler {

    @Inject
    AuditService auditService;

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (!Schema.Method.GET.isMethod(request.getMethod())){
            request.setAttribute("requestEndTime", System.currentTimeMillis());
            Policy policy = (Policy) ApiContext.getContext().getPolicy();
            auditService.logRequest(request, policy);
        }
    }
}
