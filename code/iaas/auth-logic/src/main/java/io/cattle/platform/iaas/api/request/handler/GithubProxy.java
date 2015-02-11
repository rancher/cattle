package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.github.GithubUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractResponseGenerator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class GithubProxy extends AbstractResponseGenerator {

    private static final String AUTH_HEADER = "Authorization";

    private static final DynamicBooleanProperty ALLOW_GITHUB_REDIRECT = ArchaiusUtil.getBoolean("api.allow.github.proxy");
    private static final DynamicStringProperty GITHUB_API_BASE = ArchaiusUtil.getString("api.github.base.url");

    GithubUtils githubUtils;

    @Override
    protected void generate(final ApiRequest request) throws IOException {
        if (request.getRequestVersion() == null || !ALLOW_GITHUB_REDIRECT.get())
            return;

        if (!StringUtils.equals("github", request.getRequestVersion())) {
            return;
        }

        String token = request.getServletContext().getRequest().getHeader(AUTH_HEADER);
        if (StringUtils.isEmpty(token)) {
            token = request.getServletContext().getRequest().getParameter("token");
            if (StringUtils.isEmpty(token)) {
                throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
            }
        }
        String accessToken = githubUtils.validateAndFetchGithubToken(token);
        if (StringUtils.isEmpty(accessToken)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }

        String redirect = request.getServletContext().getRequest().getRequestURI();
        redirect = redirect.substring("/github".length());
        if (StringUtils.isEmpty(redirect)) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidPath", "The github redirect path is invalid/empty", null);
        }
        Response res = Request.Get(GITHUB_API_BASE.get() + redirect).addHeader("Authorization", "token " + accessToken).addHeader("Accept", "application/json")
                .execute();
        res.handleResponse(new ResponseHandler<Object>() {

            @Override
            public Object handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                int statusCode = response.getStatusLine().getStatusCode();
                request.getServletContext().getResponse().setStatus(statusCode);
                OutputStream writer = request.getServletContext().getResponse().getOutputStream();
                Header[] headers = response.getAllHeaders();
                for (int i = 0; i < headers.length; i++) {
                    request.getServletContext().getResponse().addHeader(headers[i].getName(), headers[i].getValue());
                }
                response.getEntity().writeTo(writer);
                return null;
            }
        });

        request.commit();
        request.setResponseObject(new Object());
    }

    @Inject
    public void setGithubUtils(GithubUtils githubUtils) {
        this.githubUtils = githubUtils;
    }
}