package io.cattle.platform.docker.transform;

import io.cattle.platform.core.model.Instance;

import java.util.List;
import java.util.Map;

public interface DockerTransformer {

    void transform(Map<String, Object> fromInspect, Instance toInstance);
    
    List<DockerInspectTransformVolume> transformVolumes(Map<String, Object> fromInspect);
}
