package io.cattle.platform.api.parser;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.parser.DefaultApiRequestParser;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicBooleanProperty;

public class ApiRequestParser extends DefaultApiRequestParser {

    private static final DynamicBooleanProperty ALLOW_OVERRIDE = ArchaiusUtil.getBoolean("api.allow.client.override");

    @Override
    public boolean isAllowClientOverrideHeaders() {
        return ALLOW_OVERRIDE.get();
    }

    @Override
    public boolean parse(ApiRequest apiRequest) throws IOException {
        HttpServletRequest request = apiRequest.getServletContext().getRequest();

        String path = request.getServletPath();

        String[] parts = path.split("/");
        if (parts.length > 4 && "projects".equalsIgnoreCase(parts[2]) && !"projectMembers".equalsIgnoreCase(parts[4])) {
            String projectId = parts[3];

            apiRequest.setSubContext(String.format("/%s/%s", parts[2], projectId));

            String[] newPath = ArrayUtils.addAll(new String[]{"", parts[1]}, ArrayUtils.subarray(parts, 4, Integer.MAX_VALUE));
            String servletPath = StringUtils.join(newPath, "/");
            request = new ProjectHttpServletRequest(request, projectId, servletPath);
            apiRequest.getServletContext().setRequest(request);
        }

        return super.parse(apiRequest);
    }

}