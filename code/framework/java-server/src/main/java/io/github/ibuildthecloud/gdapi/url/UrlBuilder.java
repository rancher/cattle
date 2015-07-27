package io.github.ibuildthecloud.gdapi.url;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Sort.SortOrder;

import java.net.URL;

public interface UrlBuilder {

    public static final String SELF = "self";
    public static final String COLLECTION = "collection";
    public static final String LATEST = "latest";

    URL resourceReferenceLink(Resource resource);

    URL resourceReferenceLink(Class<?> type, String id);

    URL resourceReferenceLink(String type, String id);

    URL resourceLink(Class<?> type, String id, String name);

    URL resourceLink(Resource resource, String name);

    URL actionLink(Resource resource, String name);

    URL resourceCollection(Class<?> type);

    URL resourceCollection(String type);

    URL reverseSort(SortOrder currentOrder);

    URL sort(String field);

    URL next(String id);

    URL version(String version);

    URL current();

    URL staticResource(String... resource);

}
