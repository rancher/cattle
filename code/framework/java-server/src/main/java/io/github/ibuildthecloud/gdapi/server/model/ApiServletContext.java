package io.github.ibuildthecloud.gdapi.server.model;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ApiServletContext {

    HttpServletRequest request;
    HttpServletResponse response;
    FilterChain filterChain;

    public ApiServletContext(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        super();
        this.request = request;
        this.response = response;
        this.filterChain = filterChain;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public FilterChain getFilterChain() {
        return filterChain;
    }

    public void setFilterChain(FilterChain filterChain) {
        this.filterChain = filterChain;
    }
}
