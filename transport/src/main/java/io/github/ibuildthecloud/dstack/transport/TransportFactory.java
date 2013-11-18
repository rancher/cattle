package io.github.ibuildthecloud.dstack.transport;

import java.util.Map;


public interface TransportFactory {

    Transport getTransport(Map<String,String> properties);

}
