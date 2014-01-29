package io.github.ibuildthecloud.dstack.configitem.request;

import io.github.ibuildthecloud.dstack.configitem.model.Client;
import io.github.ibuildthecloud.dstack.configitem.model.impl.DefaultClient;
import io.github.ibuildthecloud.dstack.core.model.Agent;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

public class ConfigUpdateRequest {

    boolean deferredTrigger = false;
    long agentId;
    Client client;
    List<ConfigUpdateItem> items = new ArrayList<ConfigUpdateItem>();

    public ConfigUpdateRequest() {
    }

    public ConfigUpdateRequest(long agentId) {
        setAgentId(agentId);
    }

    public ConfigUpdateItem addItem(String name) {
        ConfigUpdateItem item = new ConfigUpdateItem(name);
        items.add(item);

        return item;
    }

    public List<ConfigUpdateItem> getItems() {
        return items;
    }

    public void setItems(List<ConfigUpdateItem> items) {
        this.items = items;
    }

    public boolean isDeferredTrigger() {
        return deferredTrigger;
    }

    public void setDeferredTrigger(boolean deferredTrigger) {
        this.deferredTrigger = deferredTrigger;
    }

    public ConfigUpdateRequest withDeferredTrigger(boolean deferredTrigger) {
        this.deferredTrigger = deferredTrigger;
        return this;
    }

    public long getAgentId() {
        return agentId;
    }

    public void setAgentId(long agentId) {
        this.agentId = agentId;
        this.client = new DefaultClient(Agent.class, agentId);
    }

    @XmlTransient
    public Client getClient() {
        return client;
    }

}
