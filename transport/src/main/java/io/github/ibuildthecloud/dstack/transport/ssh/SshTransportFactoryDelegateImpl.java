package io.github.ibuildthecloud.dstack.transport.ssh;

import java.util.Map;

import io.github.ibuildthecloud.dstack.transport.SshTransport;
import io.github.ibuildthecloud.dstack.transport.Transport;
import io.github.ibuildthecloud.dstack.transport.TransportFactoryDelegate;

public class SshTransportFactoryDelegateImpl implements TransportFactoryDelegate {

    @Override
    public Transport getTransport(Map<String,String> properties) {
        String url = properties.get("hostname");
        String username = properties.get("username");
        String password = properties.get("password");

        return new SshTransport("localhost", "docker", "docker", 22);
    }

}
