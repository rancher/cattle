package io.github.ibuildthecloud.dstack.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * Default implementation of JsonMapper that uses Jackson for marshaling and
 * supports JAXB annotations.
 *
 */
public class JacksonJsonMapper implements JsonMapper {

    ObjectMapper mapper;

    public JacksonJsonMapper() {
        mapper = new ObjectMapper();
//        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(mapper.getTypeFactory());

        AnnotationIntrospector pair = AnnotationIntrospectorPair.create(primary, secondary);
        mapper.setAnnotationIntrospector(pair);
    }

    public JacksonJsonMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> T readValue(InputStream is, Class<T> type) throws IOException {
        return mapper.readValue(is, type);
    }

    @Override
    public <T> T readValue(byte[] bytes, Class<T> type) throws IOException {
        return mapper.readValue(bytes, type);
    }

    @Override
    public <T> T readValue(String text, Class<T> type) throws IOException {
        return mapper.readValue(text, type);
    }

    @Override
    public String writeValueAsString(Object object) throws IOException {
        return mapper.writeValueAsString(object);
    }

    @Override
    public byte[] writeValueAsBytes(Object data) throws IOException {
        return mapper.writeValueAsBytes(data);
    }

    @Override
    public void writeValue(OutputStream baos, Object object) throws IOException {
        mapper.writeValue(baos, object);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T> T readCollectionValue(String content, Class<? extends Collection> collectionClass, Class<?> elementsClass)
            throws IOException {
        CollectionType type = mapper.getTypeFactory().constructCollectionType(collectionClass, elementsClass);
        return (T) mapper.readValue(content, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        if (fromValue == null)
            return null;

        if (toValueType.isAssignableFrom(fromValue.getClass()))
            return (T) fromValue;

        return mapper.convertValue(fromValue, toValueType);

    }

    public void setPrettyPrinting() {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public ObjectMapper getObjectMapper() {
        return mapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

//    @Override
//    public <T> T fromJson(String text, Class<T> type) {
//        try {
//            return readValue(text, type);
//        } catch (IOException e) {
//            throw new JsonProcessingException(e);
//        }
//    }
//
//    @Override
//    public void toJson(Object object, Writer os) {
//        try {
//            mapper.writeValue(os, object);
//        } catch (IOException e) {
//            throw new JsonProcessingException(e);
//        }
//    }
//
//    @Override
//    public String toJson(Object object) {
//        try {
//            return writeValueAsString(object);
//        } catch (IOException e) {
//            throw new JsonProcessingException(e);
//        }
//    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> readValue(InputStream is) throws IOException {
        return readValue(is, Map.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> readValue(byte[] bytes) throws IOException {
        return readValue(bytes, Map.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> readValue(String text) throws IOException {
        return readValue(text, Map.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> T readCollectionValue(InputStream is, Class<? extends Collection> collectionClass, Class<?> elementsClass)
            throws IOException {
        CollectionType type = mapper.getTypeFactory().constructCollectionType(collectionClass, elementsClass);
        return (T) mapper.readValue(is, type);
    }

}
