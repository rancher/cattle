package io.github.ibuildthecloud.gdapi.model;

import io.github.ibuildthecloud.gdapi.annotation.Type;
import io.github.ibuildthecloud.gdapi.condition.Condition;

import java.net.URL;
import java.util.List;
import java.util.Map;

@Type(list = false)
public interface Collection extends ApiStandardType {

    public static final String SORT = "sort";
    public static final String ORDER = "order";
    public static final String MARKER = "marker";
    public static final String LIMIT = "limit";
    public static final String INCLUDE = "include";

    String getType();

    String getResourceType();

    Map<String, URL> getLinks();

    List<Resource> getData();

    Map<String, URL> getCreateTypes();

    Map<String, URL> getActions();

    Map<String, URL> getSortLinks();

    Map<String, Object> getCreateDefaults();

    Pagination getPagination();

    Sort getSort();

    Map<String, List<Condition>> getFilters();

}
