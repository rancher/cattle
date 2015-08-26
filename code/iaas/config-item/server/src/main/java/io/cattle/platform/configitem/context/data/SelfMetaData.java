package io.cattle.platform.configitem.context.data;

public class SelfMetaData {
    ContainerMetaData container;
    ServiceMetaData service;
    StackMetaData stack;
    HostMetaData host;

    public SelfMetaData(ContainerMetaData container, ServiceMetaData service, StackMetaData stack, HostMetaData host) {
        super();
        this.container = container;
        this.service = service;
        this.stack = stack;
        this.host = host;
    }

    public ContainerMetaData getContainer() {
        return container;
    }

    public ServiceMetaData getService() {
        return service;
    }

    public StackMetaData getStack() {
        return stack;
    }

    public HostMetaData getHost() {
        return host;
    }
}
