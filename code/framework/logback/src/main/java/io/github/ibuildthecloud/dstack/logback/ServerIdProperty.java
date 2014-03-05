package io.github.ibuildthecloud.dstack.logback;

import io.github.ibuildthecloud.dstack.server.context.ServerContext;
import ch.qos.logback.core.PropertyDefinerBase;
import ch.qos.logback.core.spi.PropertyDefiner;

public class ServerIdProperty extends PropertyDefinerBase implements PropertyDefiner {

    @Override
    public String getPropertyValue() {
        return ServerContext.getServerId();
    }

}
