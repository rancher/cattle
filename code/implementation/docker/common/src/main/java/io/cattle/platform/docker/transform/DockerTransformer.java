package io.cattle.platform.docker.transform;

import io.cattle.platform.core.model.Instance;

import java.util.Map;

public interface DockerTransformer {

    void transform(Map<String, Object> fromInspect, Instance toInstance);
    
}
