package io.cattle.platform.configitem.context.data.metadata.common;


public class SelfMetaData {
    public static class Self {
        ContainerMetaData container;
        ServiceMetaData service;
        StackMetaData stack;
        HostMetaData host;

        public Self(ContainerMetaData container, ServiceMetaData service, StackMetaData stack, HostMetaData host) {
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

        public void setContainer(ContainerMetaData container) {
            this.container = container;
        }

        public void setService(ServiceMetaData service) {
            this.service = service;
        }

        public void setStack(StackMetaData stack) {
            this.stack = stack;
        }

        public void setHost(HostMetaData host) {
            this.host = host;
        }
    }

    Self self;

    public SelfMetaData(ContainerMetaData container, ServiceMetaData service, StackMetaData stack, HostMetaData host) {
        super();
        this.self = new Self(container, service, stack, host);
    }

    public Self getSelf() {
        return self;
    }

    public void setSelf(Self self) {
        this.self = self;
    }
}
