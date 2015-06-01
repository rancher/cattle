package io.cattle.platform.configitem.request;

import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.eventing.model.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.util.concurrent.ListenableFuture;

public class ConfigUpdateRequest {

    boolean migration = false;
    boolean deferredTrigger = false;
    Client client;
    List<ConfigUpdateItem> items = new ArrayList<ConfigUpdateItem>();
    Map<String, Object> attributes = new HashMap<String, Object>();
    ListenableFuture<? extends Event> updateFuture;

    /**
     * Do not use this constructor, only here for JSON marshalling
     */
    public ConfigUpdateRequest() {
    }

    public ConfigUpdateRequest(Client client) {
        this.client = client;
    }

    public static ConfigUpdateRequest forResource(Class<?> type, Long resourceId) {
        return new ConfigUpdateRequest(new Client(type, resourceId));
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

    @XmlTransient
    public ListenableFuture<? extends Event> getUpdateFuture() {
        return updateFuture;
    }

    public void setUpdateFuture(ListenableFuture<? extends Event> updateFuture) {
        this.updateFuture = updateFuture;
    }

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder("update [");

        boolean first = true;
        for (ConfigUpdateItem item : items) {
            if (!first) {
                message.append(", ");
            }

            first = false;
            message.append(item.getName());
        }

        message.append("] on [").append(client).append("]");

        return message.toString();
    }

}