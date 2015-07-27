package io.github.ibuildthecloud.gdapi.model;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

public interface Schema extends Resource {

    public enum Method {
        GET, PUT, DELETE, POST;

        public boolean isMethod(String value) {
            return this.toString().equals(value);
        }
    }

    String getPluralName();

    List<String> getResourceMethods();

    Map<String, Field> getResourceFields();

    Map<String, Action> getResourceActions();

    List<String> getCollectionMethods();

    Map<String, Action> getCollectionActions();

    Map<String, Field> getCollectionFields();

    Map<String, Filter> getCollectionFilters();

    List<String> getIncludeableLinks();

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    String getParent();

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    List<String> getChildren();

}
