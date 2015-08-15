package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClient;
import io.cattle.platform.iaas.api.auth.integration.github.resource.GithubClientEndpoints;
import io.cattle.platform.iaas.api.auth.integration.github.GithubUtils;
import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
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

public class GithubProxy extends AbstractResponseGenerator {

    @Inject
    GithubUtils githubUtils;

    @Inject
    GithubClient githubClient;

    @Override
    protected void generate(final ApiRequest request) throws IOException {
        if (request.getRequestVersion() == null || !GithubConstants.ALLOW_GITHUB_REDIRECT.get())
            return;

        if (!StringUtils.equals("github", request.getRequestVersion())) {
            return;
        }
        String accessToken = githubUtils.getAccessToken();
        if (accessToken == null) {
            return;
        }

        if (StringUtils.isEmpty(accessToken)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }

        String redirect = request.getServletContext().getRequest().getRequestURI();
        redirect = redirect.substring("/github".length());
        if (StringUtils.isEmpty(redirect)) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "InvalidPath", "The github redirect path is invalid/empty", null);
        }
        Response res = Request.Get(githubClient.getURL(GithubClientEndpoints.API) + redirect).addHeader("Authorization", "token " + accessToken).addHeader
                ("Accept", "application/json").execute();
        res.handleResponse(new ResponseHandler<Object>() {
            @Override
            public Object handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                int statusCode = response.getStatusLine().getStatusCode();
                request.setResponseObject(new Object());
                request.setResponseCode(statusCode);
                request.commit();
                OutputStream writer = request.getServletContext().getResponse().getOutputStream();
                Header[] headers = response.getAllHeaders();
                for (int i = 0; i < headers.length; i++) {
                    request.getServletContext().getResponse().addHeader(headers[i].getName(), headers[i].getValue());
                }
                response.getEntity().writeTo(writer);
                return null;
            }
        });
    }
}
