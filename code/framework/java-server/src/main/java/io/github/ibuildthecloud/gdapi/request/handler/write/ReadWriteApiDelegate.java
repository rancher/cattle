package io.github.ibuildthecloud.gdapi.request.handler.write;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;

import javax.servlet.ServletException;

public interface ReadWriteApiDelegate {

    void read(ApiRequest request) throws IOException;

    void write(ApiRequest request) throws IOException;

    boolean handleException(ApiRequest request, Throwable e) throws IOException, ServletException;
}
