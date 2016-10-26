package io.cattle.platform.metadata.service;

import io.cattle.platform.core.model.Instance;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.Map;

public interface MetadataService {

    Map<String, Object> getMetadataForInstance(Instance instance, IdFormatter idformatter);

    Map<String, Object> getOsMetadata(Instance instance, Map<String, Object> metadata);

}
