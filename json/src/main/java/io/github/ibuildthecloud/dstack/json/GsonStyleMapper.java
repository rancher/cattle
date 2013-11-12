package io.github.ibuildthecloud.dstack.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extends the default the DefaultJsonMapper to be more compatible with GSON.
 * Specifically this means doing serialization based on fields and not getters.
 */
public class GsonStyleMapper extends JacksonJsonMapper {

    public GsonStyleMapper() {
        super();

        ObjectMapper mapper = getObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    }

}
