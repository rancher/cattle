package io.github.ibuildthecloud.gdapi.model;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public class ListOptions {

    Sort sort;
    Pagination pagination;
    Include include;

    public ListOptions() {
    }

    public ListOptions(Sort sort, Pagination pagination, Include include) {
        super();
        this.sort = sort;
        this.pagination = pagination;
        this.include = include;
    }

    public ListOptions(ApiRequest request) {
        this.sort = request.getSort();
        this.pagination = request.getPagination();
        this.include = request.getInclude();
    }

    public Sort getSort() {
        return sort;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public Include getInclude() {
        return include;
    }

    public void setInclude(Include include) {
        this.include = include;
    }

}
