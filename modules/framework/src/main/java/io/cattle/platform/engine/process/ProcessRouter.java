package io.cattle.platform.engine.process;

import io.cattle.platform.engine.handler.ProcessHandler;

public interface ProcessRouter {

    ProcessRouter handle(String process, ProcessHandler... handler);

}
