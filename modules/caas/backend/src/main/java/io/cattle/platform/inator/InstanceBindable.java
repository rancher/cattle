package io.cattle.platform.inator;

import java.util.Map;

public interface InstanceBindable {

    void bind(InatorContext context, Map<String, Object> instanceData);

}
