package io.cattle.platform.api.parser;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.parser.DefaultApiRequestParser;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringListProperty;

public class ApiRequestParser extends DefaultApiRequestParser {

    private static final DynamicBooleanProperty ALLOW_OVERRIDE = ArchaiusUtil.getBoolean("api.allow.client.override");
    private static final DynamicStringListProperty HTTPS_PORTS = ArchaiusUtil.getList("proxy.protocol.https.ports");

    @Override
    public boolean isAllowClientOverrideHeaders() {
        return ALLOW_OVERRIDE.get();
    }

    @Override
    public boolean isHttpsPort(String host, String port) {
        if (port == null && host != null) {
            String[] parts = host.split(":", 2);
            if (parts.length > 1) {
                port = parts[1];
            } else if (ServerContext.isCustomApiHost()) {
                if (ServerContext.getHostApiBaseUrl(BaseProtocol.HTTP).startsWith("https")) {
                    port = "443";
                } else {
                    port = "80";
                }
            }
        }

        for (String p : HTTPS_PORTS.get()) {
            if (p.equals(port)) {
                return true;
            }
        }
        return false;
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