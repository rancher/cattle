package io.cattle.platform.iaas.api.container;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.iaas.api.service.RevisionDiffomatic;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class ContainerUpgradeActionHandler implements ActionHandler {
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    RevisionManager revisionManager;

    @Override
    public String getName() {
        return "instance.upgrade";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
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
