package io.cattle.platform.metadata.service;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.metadata.data.MetadataRedirectData;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;
import java.util.Map;

public interface MetadataService {

    Map<String, Object> getMetadata(Instance agentInstance, IdFormatter idformatter);

    Map<String, Object> getMetadataForInstance(Instance instance, IdFormatter idformatter);

    List<MetadataRedirectData> getMetadataRedirects(Agent agent);

    boolean isAttachMetadata(Instance instance);

    Map<String, Object> getOsMetadata(Instance instance, Map<String, Object> metadata);

}
