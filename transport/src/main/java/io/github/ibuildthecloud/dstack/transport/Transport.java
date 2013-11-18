package io.github.ibuildthecloud.dstack.transport;

import java.io.IOException;

public interface Transport {

    public void connect() throws IOException;

    String send(String data) throws IOException;

    public void disconnect();

}
