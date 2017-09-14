package io.cattle.platform.trigger;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;

public class MetadataTrigger implements Trigger {

    MetadataManager metadataManager;

    public MetadataTrigger(MetadataManager metadataManager) {
        super();
        this.metadataManager = metadataManager;
    }

    @Override
    public void trigger(Long accountId, Long clusterId, Object resource, String source) {
        if (Trigger.METADATA_SOURCE.equals(source) || (accountId == null && clusterId == null)) {
            return;
        }

        Metadata metadata;
        if (resource instanceof Account) {
            metadata = metadataManager.getMetadataForCluster(((Account) resource).getClusterId());
        } else if (accountId == null) {
            metadata = metadataManager.getMetadataForCluster(clusterId);
        } else {
            metadata = metadataManager.getMetadataForAccount(accountId);
        }
        metadata.changed(resource);
    }

}
