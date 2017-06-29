package io.cattle.platform.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

public interface JsonMapper {

    Map<String, Object> readValue(InputStream is) throws IOException;

    Map<String, Object> readValue(byte[] bytes) throws IOException;

    Map<String, Object> readValue(String text) throws IOException;

    <T> T readValue(InputStream is, Class<T> type) throws IOException;

    <T> T readValue(byte[] bytes, Class<T> type) throws IOException;

    <T> T readValue(String text, Class<T> type) throws IOException;

    String writeValueAsString(Object object) throws IOException;

    byte[] writeValueAsBytes(Object data) throws IOException;

    Map<String, Object> writeValueAsMap(Object data);

    void writeValue(OutputStream baos, Object object) throws IOException;

    @SuppressWarnings("rawtypes")
    <T> T readCollectionValue(InputStream is, Class<? extends Collection> collectionClass, Class<?> elementsClass) throws IOException;

    @SuppressWarnings("rawtypes")
    <T> T readCollectionValue(String content, Class<? extends Collection> collectionClass, Class<?> elementsClass) throws IOException;

    <T> T convertValue(Object fromValue, Class<T> toValueType);

    @SuppressWarnings("rawtypes")
    <T> T convertCollectionValue(Object fromValue, Class<? extends Collection> collectionClass, Class<?> elementsClass);
}
