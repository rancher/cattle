package io.cattle.platform.object.serialization;

import java.util.Map;

public interface ObjectTypeSerializerPostProcessor {

    String[] getTypes();

    void process(Object obj, String type, Map<String, Object> data);

}
