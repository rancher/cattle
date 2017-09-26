package io.cattle.platform.trigger;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;

import static io.cattle.platform.core.model.tables.HostTable.*;

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
            resource = instance;
            metadata = metadataManager.getMetadataForAccount(instance.getAccountId());
        } else if (resource instanceof Agent) {
            Host host = objectManager.findOne(Host.class, HOST.AGENT_ID, ((Agent) resource).getId(), HOST.REMOVED, null);
            if (host == null) {
                return;
            }
            resource = host;
            metadata = metadataManager.getMetadataForCluster(host.getClusterId());
        } else if (accountId == null) {
            metadata = metadataManager.getMetadataForCluster(clusterId);
        } else {
            metadata = metadataManager.getMetadataForAccount(accountId);
        }
        metadata.changed(resource);
    }

}
