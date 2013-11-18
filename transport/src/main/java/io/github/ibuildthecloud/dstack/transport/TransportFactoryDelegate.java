package io.github.ibuildthecloud.dstack.transport;

import java.util.Map;


public interface TransportFactoryDelegate {

    Transport getTransport(Map<String,String> properties);

}
