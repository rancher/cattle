package io.github.ibuildthecloud.gdapi.json;

import java.io.IOException;
import java.io.OutputStream;

public interface JsonMapper {

    <T> T readValue(byte[] content, Class<T> type) throws IOException;

    Object readValue(byte[] content) throws IOException;

    void writeValue(OutputStream os, Object object) throws IOException;

    <T> T convertValue(Object fromValue, Class<T> toValueType);

}
