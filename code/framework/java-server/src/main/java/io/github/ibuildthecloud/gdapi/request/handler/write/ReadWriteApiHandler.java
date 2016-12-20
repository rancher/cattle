package io.github.ibuildthecloud.gdapi.request.handler.write;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.io.IOException;

import javax.servlet.ServletException;

public class ReadWriteApiHandler extends AbstractApiRequestHandler implements ApiRequestHandler {

    ReadWriteApiDelegate delegate;

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (RequestUtils.isWriteMethod(request.getMethod())) {
            delegate.write(request);
        } else {
            delegate.read(request);
        }
    }

    @Override
    public boolean handleException(ApiRequest request, Throwable e) throws IOException, ServletException {
        return delegate.handleException(request, e);
    }

    public ReadWriteApiDelegate getDelegate() {
        return delegate;
    }

    public void setDelegate(ReadWriteApiDelegate delegate) {
        this.delegate = delegate;
    }

}
