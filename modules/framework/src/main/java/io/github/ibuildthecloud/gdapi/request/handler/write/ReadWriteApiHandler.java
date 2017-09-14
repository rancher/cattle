package io.github.ibuildthecloud.gdapi.request.handler.write;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.io.IOException;

public class ReadWriteApiHandler implements ApiRequestHandler {

    ApiRequestHandler[] delegates;
    TransactionDelegate transaction;

    public ReadWriteApiHandler(TransactionDelegate transaction, ApiRequestHandler... delegates) {
        super();
        this.delegates = delegates;
        this.transaction = transaction;
    }

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (RequestUtils.isWriteMethod(request.getMethod())) {
            transaction.doInTransactionWithException(() -> {
                for (ApiRequestHandler handler : delegates) {
                    handler.handle(request);
                }
            });
        } else {
            for (ApiRequestHandler handler : delegates) {
                handler.handle(request);
            }
        }
    }

}
