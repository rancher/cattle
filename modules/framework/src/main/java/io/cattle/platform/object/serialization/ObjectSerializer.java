package io.cattle.platform.object.serialization;

import java.util.Map;

public interface ObjectSerializer {

    Map<String, Object> serialize(Object obj);

    String getExpression();

}
