package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.metadata.service.MetadataService;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class MetadataInfoFactory extends AbstractAgentBaseContextFactory {

    private static final Logger log = LoggerFactory.getLogger(MetadataInfoFactory.class);

    MetadataService metadataService;
    JsonMapper jsonMapper;
    IdFormatter idFormatter;

    @Override
    protected void populateContext(Agent agent, Instance agentInstance, ConfigItem item, ArchiveContext context) {
        context.getData().put("metadata", getMetadata(agentInstance));
    }

    protected String getMetadata(Instance agentInstance) {
        Map<String, Object> metadata = metadataService.getMetadata(agentInstance, idFormatter);

        try {
            return jsonMapper.writeValueAsString(metadata);
        } catch (IOException e) {
            log.error("Failed to marshal metadata", e);
            return "{}";
        }
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public MetadataService getMetadataService() {
        return metadataService;
    }

    @Inject
    public void setMetadataService(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    public IdFormatter getIdFormatter() {
        return idFormatter;
    }

    @Inject
    public void setIdFormatter(IdFormatter idFormatter) {
        this.idFormatter = idFormatter;
    }

}
