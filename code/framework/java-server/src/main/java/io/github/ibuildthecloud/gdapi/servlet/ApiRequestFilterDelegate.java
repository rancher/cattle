package io.github.ibuildthecloud.gdapi.servlet;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.parser.ApiRequestParser;
import io.github.ibuildthecloud.gdapi.server.model.ApiServletContext;
import io.github.ibuildthecloud.gdapi.util.ExceptionUtils;

import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.inject.Inject;
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

    ApiRequestParser parser;
    List<ApiRequestHandler> handlers;
    boolean throwErrors = false;
    String version;
    SchemaFactory schemaFactory;
    IdFormatter idFormatter;

    public ApiContext doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return null;
        }

        if (version == null) {
            log.error("No version set");
            chain.doFilter(request, response);
            return null;
        }

        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;

        ApiRequest apiRequest = new ApiRequest(version, new ApiServletContext(httpRequest, httpResponse, chain), schemaFactory);
        apiRequest.setAttribute("requestStartTime", System.currentTimeMillis());
        ApiContext context = null;

        try {
            apiRequest.setSchemaFactory(schemaFactory);

            context = ApiContext.newContext();
            context.setApiRequest(apiRequest);

            if (idFormatter != null) {
                context.setIdFormatter(idFormatter);
            }

            if (!parser.parse(apiRequest)) {
                chain.doFilter(httpRequest, httpResponse);
                return null;
            }

            URL schemaUrl = ApiContext.getUrlBuilder().resourceCollection(Schema.class);
            if (schemaUrl != null) {
                httpResponse.setHeader(SCHEMAS_HEADER, schemaUrl.toExternalForm());
            }

            Throwable currentError = null;
            for (ApiRequestHandler handler : handlers) {
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
            if (throwErrors) {
                ExceptionUtils.rethrowRuntime(t);
                ExceptionUtils.rethrow(t, IOException.class);
                ExceptionUtils.rethrow(t, ServletException.class);
                throw new ServletException(t);
            } else {
                if (!httpResponse.isCommitted()) {
                    httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        } finally {
            apiRequest.commit();
            ApiContext.remove();
        }

        return context;
    }

    public ApiRequestParser getParser() {
        return parser;
    }

    @Inject
    public void setParser(ApiRequestParser parser) {
        this.parser = parser;
    }

    public boolean isThrowErrors() {
        return throwErrors;
    }

    public void setThrowErrors(boolean throwErrors) {
        this.throwErrors = throwErrors;
    }

    public List<ApiRequestHandler> getHandlers() {
        return handlers;
    }

    @Inject
    public void setHandlers(List<ApiRequestHandler> handlers) {
        this.handlers = handlers;
    }

    public String getVersion() {
        return version;
    }

    @Inject
    public void setVersion(String version) {
        this.version = version;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public IdFormatter getIdFormatter() {
        return idFormatter;
    }

    public void setIdFormatter(IdFormatter idFormatter) {
        this.idFormatter = idFormatter;
    }

}
