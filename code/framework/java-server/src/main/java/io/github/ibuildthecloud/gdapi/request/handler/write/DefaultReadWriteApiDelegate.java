package io.github.ibuildthecloud.gdapi.request.handler.write;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

public class DefaultReadWriteApiDelegate implements ReadWriteApiDelegate {

    List<ApiRequestHandler> handlers;

    @Override
    public void read(ApiRequest request) throws IOException {
        handle(request);
    }

    @Override
    public void write(ApiRequest request) throws IOException {
        handle(request);
    }

    protected void handle(ApiRequest request) throws IOException {
        for (ApiRequestHandler handler : handlers) {
            handler.handle(request);
        }
    }

    public List<ApiRequestHandler> getHandlers() {
        return handlers;
    }

    @Override
    public void setHandlers(List<ApiRequestHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public boolean handleException(ApiRequest request, Throwable e) throws IOException, ServletException {
        for (ApiRequestHandler handler : handlers) {
            if (handler.handleException(request, e)) {
                return true;
            }
        }
        return false;
    }

}
