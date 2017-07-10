package io.github.ibuildthecloud.gdapi.model;

import io.github.ibuildthecloud.gdapi.annotation.Type;
import io.github.ibuildthecloud.gdapi.condition.Condition;

import java.net.URL;
import java.util.List;
import java.util.Map;

@Type(list = false)
public interface Collection extends ApiStandardType {

    String SORT = "sort";
    String ORDER = "order";
    String MARKER = "marker";
    String LIMIT = "limit";
    String INCLUDE = "include";

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
