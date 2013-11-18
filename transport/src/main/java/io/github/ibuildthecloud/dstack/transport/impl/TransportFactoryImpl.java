package io.github.ibuildthecloud.dstack.transport.impl;

import io.github.ibuildthecloud.dstack.transport.Transport;
import io.github.ibuildthecloud.dstack.transport.TransportFactory;
import io.github.ibuildthecloud.dstack.transport.TransportFactoryDelegate;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class TransportFactoryImpl implements TransportFactory {

    List<TransportFactoryDelegate> delegates;

    @Override
    public Transport getTransport(Map<String,String> properties) {
        for ( TransportFactoryDelegate delegate : delegates ) {
            Transport transport = delegate.getTransport(properties);
            if ( transport != null )
                return transport;
        }
        throw new IllegalStateException("Failed to find transport for host [" + properties + "]");
    }

    public List<TransportFactoryDelegate> getDelegates() {
        return delegates;
    }

    @Inject
    public void setDelegates(List<TransportFactoryDelegate> delegates) {
        this.delegates = delegates;
    }

}
