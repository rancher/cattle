package io.github.ibuildthecloud.gdapi.json;

import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.SchemaCollection;
import io.github.ibuildthecloud.gdapi.model.impl.FieldImpl;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;
import io.github.ibuildthecloud.gdapi.util.DateUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonMapper implements JsonMapper {

    ObjectMapper mapper;
    boolean escapeForwardSlashes = true;

    public JacksonMapper() {
        init();
    }

    public void init() {
        SimpleModule module = getModule();

        SimpleDateFormat df = new SimpleDateFormat(DateUtils.DATE_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        mapper = new ObjectMapper();
        mapper.setDateFormat(df);
        mapper.registerModule(module);
        mapper.getFactory().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (escapeForwardSlashes) {
            mapper.getFactory().setCharacterEscapes(new EscapeForwardSlash());
        }
    }

    protected SimpleModule getModule() {
        SimpleModule module = new SimpleModule();
        module.setMixInAnnotation(Resource.class, ResourceMix.class);
        module.setMixInAnnotation(SchemaCollection.class, SchemaCollectionMixin.class);
        module.setMixInAnnotation(SchemaImpl.class, SchemaImplMixin.class);
        return module;
    }

    @Override
    public <T> T readValue(byte[] content, Class<T> type) throws IOException {
        return mapper.readValue(content, type);
    }

    @Override
    public Object readValue(byte[] content) throws IOException {
        return mapper.readValue(content, Object.class);
    }

    @Override
    public void writeValue(OutputStream os, Object object) throws IOException {
        mapper.writeValue(os, object);
    }

    @Override
    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return mapper.convertValue(fromValue, toValueType);
    }

    public static interface ResourceMix {
        @JsonAnyGetter
        Map<String, Object> getFields();
        @JsonInclude(Include.NON_NULL)
        String getBaseType();
    }

    public static interface SchemaCollectionMixin {
        @JsonDeserialize(as = List.class, contentAs = SchemaImpl.class)
        List<Schema> getData();
    }

    public static interface SchemaImplMixin {
        @JsonDeserialize(as = Map.class, contentAs = FieldImpl.class)
        Map<String, Field> getResourceFields();
    }

    public static interface ResourceMixin {
        @JsonAnyGetter
        Map<String, Object> getFields();
    }

    public boolean isEscapeForwardSlashes() {
        return escapeForwardSlashes;
    }

    public void setEscapeForwardSlashes(boolean escapeForwardSlashes) {
        this.escapeForwardSlashes = escapeForwardSlashes;
    }

}
