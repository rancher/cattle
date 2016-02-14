package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.configitem.context.dao.MetaDataInfoDao.Version;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;

public class SelfMetaData {

    public static class Self {
        ContainerMetaData container;
        ServiceMetaData service;
        StackMetaData stack;
        HostMetaData host;

        public Self(ContainerMetaData container, ServiceMetaData service, StackMetaData stack, HostMetaData host,
                Version version) {
            super();
            this.container = container;
            if(service != null){
                this.service = ServiceMetaData.getServiceMetaData(service, version);
                this.service.setToken(DataAccessor.fieldString(service.getService(),
                        ServiceDiscoveryConstants.FIELD_TOKEN));
            }
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

    public SelfMetaData(ContainerMetaData container, ServiceMetaData service, StackMetaData stack, HostMetaData host,
            Version version) {
        super();
        this.self = new Self(container, service, stack, host, version);
    }

    public Self getSelf() {
        return self;
    }

    public void setSelf(Self self) {
        this.self = self;
    }
}
