package io.cattle.platform.logback;

import io.cattle.platform.server.context.ServerContext;
import ch.qos.logback.core.PropertyDefinerBase;
import ch.qos.logback.core.spi.PropertyDefiner;

public class ServerIdProperty extends PropertyDefinerBase implements PropertyDefiner {

    @Override
    public String getPropertyValue() {
        return ServerContext.getServerId();
    }

}
