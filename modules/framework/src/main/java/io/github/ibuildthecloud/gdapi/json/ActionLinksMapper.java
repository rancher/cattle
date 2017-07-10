package io.github.ibuildthecloud.gdapi.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.SchemaCollection;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import java.net.URL;
import java.util.Map;

public class ActionLinksMapper extends JacksonMapper {

    ObjectMapper mapper;
    boolean escapeForwardSlashes;

    public ActionLinksMapper() {
        init();
    }

    @Override
    protected SimpleModule getModule() {
        SimpleModule module = new SimpleModule();
        module.setMixInAnnotation(Resource.class, ResourceMix.class);
        module.setMixInAnnotation(Collection.class, ResourceMix.class);
        module.setMixInAnnotation(SchemaCollection.class, SchemaCollectionMixin.class);
        module.setMixInAnnotation(SchemaImpl.class, SchemaImplMixin.class);
        return module;
    }


    public interface ResourceMix {
        @JsonAnyGetter
        Map<String, Object> getFields();
        @JsonProperty("actionLinks")
        Map<String, URL> getActions();
    }

}