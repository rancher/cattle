package io.cattle.platform.configitem.request;

import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.impl.DefaultClient;
import io.cattle.platform.core.model.Agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

public class ConfigUpdateRequest {

    boolean migration = false;
    boolean deferredTrigger = false;
    long agentId;
    Client client;
    List<ConfigUpdateItem> items = new ArrayList<ConfigUpdateItem>();
    Map<String,Object> attributes = new HashMap<String, Object>();

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

    public boolean isMigration() {
        return migration;
    }

    public void setMigration(boolean migration) {
        this.migration = migration;
    }

    public ConfigUpdateRequest withMigration(boolean migration) {
        this.migration = migration;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

}