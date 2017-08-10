package io.cattle.platform.api.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.revision.RevisionDiffomatic;
import io.cattle.platform.revision.RevisionManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

public class ContainerUpgradeActionHandler implements ActionHandler {

    ObjectManager objectManager;
    ObjectProcessManager objectProcessManager;
    RevisionManager revisionManager;

    public ContainerUpgradeActionHandler(ObjectManager objectManager, ObjectProcessManager objectProcessManager, RevisionManager revisionManager) {
        super();
        this.objectManager = objectManager;
        this.objectProcessManager = objectProcessManager;
        this.revisionManager = revisionManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {
        Instance instance = (Instance)obj;

        RevisionDiffomatic diff = revisionManager.createNewRevision(request.getSchemaFactory(),
                instance,
                CollectionUtils.toMap(
                        CollectionUtils.toMap(request.getRequestObject()).get(InstanceConstants.FIELD_REVISION_CONFIG)));

        if (diff != null && diff.isCreateRevision()) {
            Revision rev = revisionManager.assignRevision(diff, instance);
            if (rev != null) {
                return rev;
            }
        }

        return objectManager.loadResource(Revision.class, instance.getRevisionId());
    }

}
