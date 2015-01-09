package io.cattle.platform.api.pubsub.subscribe;

import java.io.OutputStream;

import javax.servlet.AsyncContext;

public class AsyncOutputStreamMessageWriter extends OutputStreamMessageWriter {

    AsyncContext ctx;

    public AsyncOutputStreamMessageWriter(OutputStream os, AsyncContext ctx) {
        super(os);
        this.ctx = ctx;
    }

    @Override
    public void close() {
        if (ctx != null) {
            ctx.complete();
        }

        ctx = null;

        super.close();
    }

}