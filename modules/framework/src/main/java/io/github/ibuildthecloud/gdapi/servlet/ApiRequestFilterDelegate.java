package io.github.ibuildthecloud.gdapi.servlet;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.ApiRouter;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.parser.ApiRequestParser;
import io.github.ibuildthecloud.gdapi.server.model.ApiServletContext;
import io.github.ibuildthecloud.gdapi.version.Versions;

import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiRequestFilterDelegate {

    public static final String SCHEMAS_HEADER = "X-API-Schemas";

    private static final Logger log = LoggerFactory.getLogger(ApiRequestFilterDelegate.class);

    Versions versions;
    ApiRequestParser parser;
    ApiRouter apiRouter;
    Map<String, SchemaFactory> schemaFactories;
    IdFormatter idFormatter;

    public ApiRequestFilterDelegate(Versions versions, ApiRequestParser parser, ApiRouter router, Map<String, SchemaFactory> schemaFactories,
            IdFormatter idFormatter) {
        this.versions = versions;
        this.parser = parser;
        this.apiRouter = router;
        this.schemaFactories = schemaFactories;
        this.idFormatter = idFormatter;
    }

    public ApiContext doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return null;
        }

        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;

        String version = parser.parseVersion(httpRequest.getServletPath());
        SchemaFactory schemaFactory = schemaFactories.get(version);

        ApiRequest apiRequest = new ApiRequest(new ApiServletContext(httpRequest, httpResponse, chain), schemaFactory);
        apiRequest.setAttribute("requestStartTime", System.currentTimeMillis());
        ApiContext context = null;

        try {
            context = ApiContext.newContext();
            context.setApiRequest(apiRequest);

            if (idFormatter != null) {
                context.setIdFormatter(idFormatter);
            }

            if (!parser.parse(apiRequest)) {
                chain.doFilter(httpRequest, httpResponse);
                return null;
            }

            URL schemaUrl = null;
            if (schemaFactory == null) {
                // Not a valid version
                SchemaFactory defaultFactory = schemaFactories.get(versions.getLatest());
                if (apiRequest.getVersion() == null) {
                    apiRequest.setVersion(versions.getRootVersion());
                } else {
                    apiRequest.setVersion(versions.getLatest());
                }
                apiRequest.setSchemaFactory(defaultFactory);
                schemaUrl = ApiContext.getUrlBuilder().resourceCollection(Schema.class);
            } else {
                schemaUrl = ApiContext.getUrlBuilder().resourceCollection(Schema.class);
            }

            if (schemaUrl != null) {
                httpResponse.setHeader(SCHEMAS_HEADER, schemaUrl.toExternalForm());
            }

            Throwable currentError = null;
            for (ApiRequestHandler handler : apiRouter.getHandlers()) {
                try {
                    if (currentError == null) {
                        handler.handle(apiRequest);
                    } else {
                        if (handler.handleException(apiRequest, currentError)) {
                            currentError = null;
                        }
                    }
                } catch (EOFException e) {
                    throw e;
                } catch (Throwable t) {
                    currentError = t;
                    apiRequest.getExceptions().add(t);
                    try {
                        if (handler.handleException(apiRequest, currentError)) {
                            currentError = null;
                        }
                    } catch (Throwable t1) {
                        currentError = t1;
                    }
                }
            }
            if (currentError != null) {
                throw currentError;
            }
        } catch (EOFException e) {
            log.trace("Caught EOFException, ignoring", e);
            throw e;
        } catch (Throwable t) {
            log.error("Unhandled exception in API for request [{}]", apiRequest, t);
            if (!httpResponse.isCommitted()) {
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } finally {
            apiRequest.commit();
            ApiContext.remove();
        }

        return context;
    }

}
