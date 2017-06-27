package io.github.ibuildthecloud.gdapi.model;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public class ListOptions {

    Sort sort;
    Pagination pagination;

    public ListOptions() {
    }

    public ListOptions(Sort sort, Pagination pagination) {
        super();
        this.sort = sort;
        this.pagination = pagination;
    }

    public ListOptions(ApiRequest request) {
        this.sort = request.getSort();
        this.pagination = request.getPagination();
    }

    public Sort getSort() {
        return sort;
    }

    public Pagination getPagination() {
        return pagination;
    }

}
