package io.github.ibuildthecloud.gdapi.request.handler.write;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletException;

public class DefaultReadWriteApiDelegate implements ReadWriteApiDelegate {

    List<ApiRequestHandler> handlers;
    @Inject
    TransactionDelegate transactionDelegate;

    @Override
    public void read(ApiRequest request) throws IOException {
        handle(request);
    }

    @Override
    public void write(ApiRequest request) throws IOException {
        transactionDelegate.doInTransactionWithException(() -> {
            handle(request);
        });
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
