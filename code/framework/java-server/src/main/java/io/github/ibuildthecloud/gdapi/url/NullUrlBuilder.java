package io.github.ibuildthecloud.gdapi.url;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Sort.SortOrder;

import java.net.URL;

public class NullUrlBuilder implements UrlBuilder {

    @Override
    public URL resourceReferenceLink(Resource resource) {
        return null;
    }

    @Override
    public URL resourceCollection(Class<?> type) {
        return null;
    }

    @Override
    public URL resourceCollection(String type) {
        return null;
    }

    @Override
    public URL resourceLink(Resource resource, String name) {
        return null;
    }

    @Override
    public URL reverseSort(SortOrder currentOrder) {
        return null;
    }

    @Override
    public URL sort(String field) {
        return null;
    }

    @Override
    public URL next(String id) {
        return null;
    }

    @Override
    public URL version(String version) {
        return null;
    }

    @Override
    public URL current() {
        return null;
    }

    @Override
    public URL resourceReferenceLink(Class<?> type, String id) {
        return null;
    }

    @Override
    public URL resourceReferenceLink(String type, String id) {
        return null;
    }

    @Override
    public URL actionLink(Resource resource, String name) {
        return null;
    }

    @Override
    public URL staticResource(String... resources) {
        return null;
    }

    @Override
    public URL resourceLink(Class<?> type, String id, String name) {
        return null;
    }

}
