package io.cattle.platform.trigger;

import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.metadata.service.Metadata;

public class MetadataTrigger implements Trigger {

    EnvironmentResourceManager environmentResourceManager;

    public MetadataTrigger(EnvironmentResourceManager environmentResourceManager) {
        super();
        this.environmentResourceManager = environmentResourceManager;
    }

    @Override
    public void trigger(Long accountId, Object resource, String source) {
        if (Trigger.METADATA_SOURCE.equals(source) || accountId == null) {
            return;
        }

        Metadata metadata = environmentResourceManager.getMetadata(accountId);
        metadata.changed(resource);
    }

}
