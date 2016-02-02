package io.github.ibuildthecloud.gdapi.model.impl;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Action;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Filter;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlTransient;

public class SchemaImpl extends ResourceImpl implements Schema {

    String name, parent;
    String pluralName;
    List<String> children = new ArrayList<String>();
    boolean create, update, list = true, deletable, byId = true;
    Map<String, Field> resourceFields = new TreeMap<String, Field>();
    Map<String, Filter> collectionFilters = new TreeMap<String, Filter>();
    List<String> includeableLinks = new ArrayList<String>();
    Map<String, Action> resourceActions = new HashMap<String, Action>();
    Map<String, Action> collectionActions = new HashMap<String, Action>();
    Map<String, Field> collectionFields = new HashMap<String, Field>();

    public SchemaImpl() {
        setType("schema");
    }

    public SchemaImpl(SchemaImpl schema) {
        this();
        this.name = schema.getId();
        this.parent = schema.getParent();
        this.children = new ArrayList<String>(schema.getChildren());
        this.pluralName = schema.getPluralName();

        this.load(schema);
    }

    public void load(SchemaImpl schema) {
        this.create = schema.isCreate();
        this.update = schema.isUpdate();
        this.list = schema.isList();
        this.deletable = schema.isDeletable();
        this.byId = schema.isById();
        this.includeableLinks = new ArrayList<String>(schema.getIncludeableLinks());
        this.resourceFields = copyFields(schema.getResourceFields());
        this.collectionFilters = copyFilters(schema.getCollectionFilters());
        this.resourceActions = copyActions(schema.getResourceActions());
        this.collectionActions = copyActions(schema.getCollectionActions());
        this.collectionFields = copyFields(schema.getCollectionFields());
    }

    protected Map<String, Field> copyFields(Map<String, Field> input) {
        Map<String, Field> result = new LinkedHashMap<String, Field>();
        for (String key : input.keySet()) {
            result.put(key, new FieldImpl(input.get(key)));
        }

        return result;
    }

    protected Map<String, Filter> copyFilters(Map<String, Filter> input) {
        Map<String, Filter> result = new LinkedHashMap<String, Filter>();
        for (String key : input.keySet()) {
            result.put(key, new Filter(input.get(key)));
        }

        return result;
    }

    protected Map<String, Action> copyActions(Map<String, Action> input) {
        Map<String, Action> result = new LinkedHashMap<String, Action>();
        for (String key : input.keySet()) {
            result.put(key, new Action(input.get(key)));
        }

        return result;
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public void setId(String name) {
        this.name = name;
    }

    @Override
    public Map<String, Field> getResourceFields() {
        return resourceFields;
    }

    public void setResourceFields(Map<String, Field> resourceFields) {
        this.resourceFields = resourceFields;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlTransient
    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    @XmlTransient
    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    @XmlTransient
    public boolean isList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
    }

    public boolean isDeletable() {
        return deletable;
    }

    @XmlTransient
    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    @XmlTransient
    public boolean isById() {
        return byId;
    }

    public void setById(boolean byId) {
        this.byId = byId;
    }

    public void setResourceMethods(List<String> resourceMethods) {
        if (resourceMethods == null) {
            byId = false;
            update = false;
            deletable = false;
            return;
        }

        byId = resourceMethods.contains(Method.GET.toString());
        update = resourceMethods.contains(Method.PUT.toString());
        deletable = resourceMethods.contains(Method.DELETE.toString());
    }

    @Override
    public List<String> getResourceMethods() {
        List<String> methods = new ArrayList<String>();

        if (byId) {
            methods.add(Method.GET.toString());
        }

        if (update) {
            methods.add(Method.PUT.toString());
        }

        if (deletable) {
            methods.add(Method.DELETE.toString());
        }

        return methods;
    }

    public void setCollectionMethods(List<String> collectionMethods) {
        if (collectionMethods == null) {
            list = false;
            create = false;
            return;
        }

        list = collectionMethods.contains(Method.GET.toString());
        create = collectionMethods.contains(Method.POST.toString());
    }

    @Override
    public List<String> getCollectionMethods() {
        List<String> methods = new ArrayList<String>();

        if (list) {
            methods.add(Method.GET.toString());
        }

        if (create) {
            methods.add(Method.POST.toString());
        }

        return methods;
    }

    @Override
    public Map<String, URL> getLinks() {
        Map<String, URL> result = links;
        if (!links.containsKey(UrlBuilder.SELF)) {
            result = new HashMap<String, URL>(links);
            result.put(UrlBuilder.SELF, ApiContext.getUrlBuilder().resourceReferenceLink(this));
        }

        if (list && !result.containsKey(UrlBuilder.COLLECTION)) {
            result = result == null ? new HashMap<String, URL>(links) : result;
            result.put(UrlBuilder.COLLECTION, ApiContext.getUrlBuilder().resourceCollection(getId()));
        }

        return result == null ? links : result;
    }

    @Override
    public Map<String, Action> getResourceActions() {
        return resourceActions;
    }

    @Override
    public Map<String, Action> getCollectionActions() {
        return collectionActions;
    }

    @Override
    public Map<String, Field> getCollectionFields() {
        return collectionFields;
    }

    @Override
    public Map<String, Filter> getCollectionFilters() {
        return collectionFilters;
    }

    @Override
    public String getPluralName() {
        if (pluralName == null)
            return TypeUtils.guessPluralName(name);
        return pluralName;
    }

    public void setPluralName(String pluralName) {
        this.pluralName = pluralName;
    }

    @XmlTransient
    public String getRawPluralName() {
        return pluralName;
    }

    @Override
    public List<String> getIncludeableLinks() {
        return includeableLinks;
    }

    public void setIncludeableLinks(List<String> includeableLinks) {
        this.includeableLinks = includeableLinks;
    }

    @Override
    public String toString() {
        return "SchemaImpl [name=" + name + "]";
    }

    public void setCollectionFilters(Map<String, Filter> collectionFilters) {
        this.collectionFilters = collectionFilters;
    }

    public void setCollectionActions(Map<String, Action> collectionActions) {
        this.collectionActions = collectionActions;
    }

    public void setCollectionFields(Map<String, Field> collectionFields) {
        this.collectionFields = collectionFields;
    }

    public void setResourceActions(Map<String, Action> resourceActions) {
        this.resourceActions = resourceActions;
    }

    @Override
    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    @Override
    public List<String> getChildren() {
        return children;
    }

}
