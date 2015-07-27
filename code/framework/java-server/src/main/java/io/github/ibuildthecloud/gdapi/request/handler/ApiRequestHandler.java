package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

import javax.servlet.ServletException;

public interface ApiRequestHandler {

    void handle(ApiRequest request) throws IOException;

    boolean handleException(ApiRequest request, Throwable e) throws IOException, ServletException;

}
