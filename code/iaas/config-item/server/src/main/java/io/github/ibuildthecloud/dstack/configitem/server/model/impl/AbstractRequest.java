package io.github.ibuildthecloud.dstack.configitem.server.model.impl;

import io.github.ibuildthecloud.dstack.configitem.model.Client;
import io.github.ibuildthecloud.dstack.configitem.model.ItemVersion;
import io.github.ibuildthecloud.dstack.configitem.server.model.Request;

import java.util.Map;

import javax.inject.Inject;

public abstract class AbstractRequest implements Request {

    String itemName;
    int responseCode = Request.OK;
    Client client;
    ItemVersion appliedVersion;
    Map<String, Object> params;

    public AbstractRequest() {
    }

    public AbstractRequest(String itemName, Client client, ItemVersion appliedVersion, Map<String, Object> params) {
        this.itemName = itemName;
        this.client = client;
        this.appliedVersion = appliedVersion;
        this.params = params;
    }

    @Override
    public ItemVersion getAppliedVersion() {
        return appliedVersion;
    }

    @Override
    public String getItemName() {
        return itemName;
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public void setResponseCode(int code) {
        this.responseCode = code;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setAppliedVersion(ItemVersion appliedVersion) {
        this.appliedVersion = appliedVersion;
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Inject
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public void setContentType(String contentType) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [itemName=" + itemName + ", client=" + client + ", appliedVersion=" + appliedVersion
                + ", params=" + params + "]";
    }

}
