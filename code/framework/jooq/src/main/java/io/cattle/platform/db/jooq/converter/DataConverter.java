package io.cattle.platform.db.jooq.converter;

import io.cattle.platform.db.jooq.converter.impl.JsonUnmodifiableMap;
import io.cattle.platform.json.JacksonJsonMapper;
import io.cattle.platform.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

import org.jooq.Converter;

public class DataConverter implements Converter<String, Map<String, Object>> {

    private static final long serialVersionUID = 8496703546902403178L;

    JsonMapper mapper = new JacksonJsonMapper();

    @Override
    public Map<String, Object> from(String databaseObject) {
        if (databaseObject == null) {
            return null;
        }

        return new JsonUnmodifiableMap<String, Object>(mapper, databaseObject);
    }

    @Override
    public String to(Map<String, Object> userObject) {
        if (userObject == null) {
            return null;
        }
        try {
            String result = mapper.writeValueAsString(userObject);
            if (userObject instanceof JsonUnmodifiableMap<?, ?>) {
                ((JsonUnmodifiableMap<?, ?>) userObject).setText(result);
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to marshall [" + userObject + "]", e);
        }
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Map<String, Object>> toType() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

}