package io.github.ibuildthecloud.gdapi.request.handler;

import java.io.IOException;

import javax.servlet.ServletException;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface ApiRequestHandler {

    void handle(ApiRequest request) throws IOException;

    boolean handleException(ApiRequest request, Throwable e) throws IOException, ServletException;

}
