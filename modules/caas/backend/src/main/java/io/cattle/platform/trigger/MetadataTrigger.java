package io.cattle.platform.trigger;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;

public class MetadataTrigger implements Trigger {

    MetadataManager metadataManager;
    ObjectManager objectManager;

    public MetadataTrigger(MetadataManager metadataManager, ObjectManager objectManager) {
        super();
        this.metadataManager = metadataManager;
        this.objectManager = objectManager;
    }

    @Override
    public void trigger(Long accountId, Long clusterId, Object resource, String source) {
        if (Trigger.METADATA_SOURCE.equals(source) || (accountId == null && clusterId == null)) {
            return;
        }

        Metadata metadata;
        if (resource instanceof Account) {
            metadata = metadataManager.getMetadataForCluster(((Account) resource).getClusterId());
        } else if (resource instanceof ServiceEvent) {
            Instance instance = objectManager.loadResource(Instance.class, ((ServiceEvent) resource).getInstanceId());
            metadata = metadataManager.getMetadataForAccount(instance.getAccountId());
        } else if (accountId == null) {
            metadata = metadataManager.getMetadataForCluster(clusterId);
        } else {
            metadata = metadataManager.getMetadataForAccount(accountId);
        }
        metadata.changed(resource);
    }

}
