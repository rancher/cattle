package io.cattle.platform.configitem.model;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ConfigItemStatus;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.iaas.event.IaasEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    Class<?> resourceType;
    long resourceId;
    String eventName;

    public Client() {
    }

    public Client(ConfigItemStatus status) {
        if (status.getAgentId() != null) {
            resourceType = Agent.class;
            resourceId = status.getAgentId();
        } else if (status.getAccountId() != null) {
            resourceType = Account.class;
            resourceId = status.getAccountId();
        } else if (status.getServiceId() != null) {
            resourceType = Service.class;
            resourceId = status.getServiceId();
        } else if (status.getStackId() != null) {
            resourceType = Stack.class;
            resourceId = status.getStackId();
        } else if (status.getHostId() != null) {
            resourceType = Host.class;
            resourceId = status.getHostId();
        }

        assignEvent(status.getName());
    }

    public Client(Class<?> resourceType, long resourceId) {
        super();
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        assignEvent(null);
    }

    protected void assignEvent(String configItemStatusName) {
        if (resourceType == Agent.class) {
            eventName = IaasEvents.CONFIG_UPDATE;
        } else if (resourceType == Account.class) {
            eventName = IaasEvents.GLOBAL_SERVICE_UPDATE;
        } else if (resourceType == Service.class) {
            eventName = IaasEvents.SERVICE_UPDATE;
        } else if (resourceType == Stack.class) {
            eventName = IaasEvents.STACK_UPDATE;
        } else if (resourceType == Host.class) {
            eventName = IaasEvents.HOST_ENDPOINTS_UPDATE;
        } else {
            log.error("Failed to assign event name for client");
        }
    }

    public Class<?> getResourceType() {
        return resourceType;
    }

    public void setResourceType(Class<?> resourceType) {
        this.resourceType = resourceType;
    }

    public long getResourceId() {
        return resourceId;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public String toString() {
        return (resourceType.getSimpleName() + ":" + resourceId).toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Client that = (Client) o;

        if (resourceId != that.resourceId) return false;
        return !(resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null);

    }

    @Override
    public int hashCode() {
        int result = resourceType != null ? resourceType.hashCode() : 0;
        result = 31 * result + (int) (resourceId ^ (resourceId >>> 32));
        return result;
    }
}
