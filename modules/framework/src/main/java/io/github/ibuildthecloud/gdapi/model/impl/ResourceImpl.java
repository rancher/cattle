package io.github.ibuildthecloud.gdapi.model.impl;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class ResourceImpl implements Resource {

    String id, type;
    Map<String, URL> links = new LinkedHashMap<String, URL>();
    Map<String, URL> actions = new LinkedHashMap<String, URL>();
    Map<String, Object> fields = new TreeMap<String, Object>();

    public ResourceImpl() {
    }

    public ResourceImpl(String id, String type, Map<String, Object> fields) {
        this.id = id;
        this.type = type;
        this.fields = fields;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getBaseType() {
        return getType();
    }

    @Override
    public Map<String, URL> getLinks() {
        if (!links.containsKey(UrlBuilder.SELF)) {
            URL self = ApiContext.getUrlBuilder().resourceReferenceLink(this);
            if (self != null) {
                links.put(UrlBuilder.SELF, self);
            }
        }
        return links;
    }

    @Override
    public Map<String, URL> getActions() {
        return actions;
    }

    @Override
    public Map<String, Object> getFields() {
        return fields;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLinks(Map<String, URL> links) {
        this.links = links;
    }

    public void setActions(Map<String, URL> actions) {
        this.actions = actions;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

}
