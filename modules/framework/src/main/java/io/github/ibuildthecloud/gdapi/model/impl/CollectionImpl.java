package io.github.ibuildthecloud.gdapi.model.impl;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Pagination;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Sort;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CollectionImpl implements Collection {

    String type = "collection";
    String resourceType;
    Map<String, URL> links = new HashMap<String, URL>();
    Map<String, URL> createTypes = new HashMap<String, URL>();
    Map<String, URL> actions = new HashMap<String, URL>();
    List<Resource> data = new LinkedList<Resource>();
    Map<String, URL> sortLinks = new HashMap<String, URL>();
    Pagination pagination;
    Sort sort;
    Map<String, List<Condition>> filters = new HashMap<String, List<Condition>>();
    Map<String, Object> createDefaults = new HashMap<String, Object>();

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    @Override
    public Map<String, URL> getLinks() {
        if (!links.containsKey(UrlBuilder.SELF)) {
            URL self = ApiContext.getUrlBuilder().current();
            if (self != null) {
                links.put(UrlBuilder.SELF, self);
            }
        }

        return links;
    }

    @Override
    public List<Resource> getData() {
        return data;
    }

    @Override
    public Map<String, URL> getCreateTypes() {
        return createTypes;
    }

    @Override
    public Map<String, URL> getActions() {
        return actions;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setLinks(Map<String, URL> links) {
        this.links = links;
    }

    public void setCreateTypes(Map<String, URL> createTypes) {
        this.createTypes = createTypes;
    }

    public void setActions(Map<String, URL> actions) {
        this.actions = actions;
    }

    public void setData(List<Resource> data) {
        this.data = data;
    }

    @Override
    public Map<String, URL> getSortLinks() {
        return sortLinks;
    }

    public void setSortLinks(Map<String, URL> sortLinks) {
        this.sortLinks = sortLinks;
    }

    @Override
    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    @Override
    public Map<String, List<Condition>> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, List<Condition>> filters) {
        this.filters = filters;
    }

    @Override
    public Map<String, Object> getCreateDefaults() {
        return createDefaults;
    }

    public void setCreateDefaults(Map<String, Object> createDefaults) {
        this.createDefaults = createDefaults;
    }

}
