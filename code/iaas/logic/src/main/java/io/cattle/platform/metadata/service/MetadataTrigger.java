package io.cattle.platform.metadata.service;

import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.object.util.ObjectUtils;

public class MetadataTrigger implements Trigger {

    EnvironmentResourceManager environmentResourceManager;

    public MetadataTrigger(EnvironmentResourceManager environmentResourceManager) {
        super();
        this.environmentResourceManager = environmentResourceManager;
    }

    @Override
    public void trigger(ProcessInstance process) {
        Object obj = process.getResource();
        Object accountId = ObjectUtils.getAccountId(obj);
        if (!(accountId instanceof Long)) {
            return;
        }

        Metadata metadata = environmentResourceManager.getMetadata((Long)accountId);
        metadata.changed(obj);
    }

}
