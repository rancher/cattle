package io.cattle.platform.iaas.api.service;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.core.model.Service;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.Map;

public interface RevisionManager {

    Revision createInitialRevision(Service service, Map<String, Object> data);

    void createInitialRevisionForInstance(long accountId, Map<String, Object> data);

    RevisionDiffomatic createNewRevision(SchemaFactory factory, Service service, Map<String, Object> data);

    RevisionDiffomatic createNewRevision(SchemaFactory factory, Instance instance, Map<String, Object> data);

    Service assignRevision(RevisionDiffomatic diffomatic, Service service);

    Revision assignRevision(RevisionDiffomatic diffomatic, Instance service);

    void setFieldsForUpgrade(Map<String, Object> data);

    Map<String, Object> getServiceDataForRollback(Service service, Long targetRevisionId);

    Service convertToService(Instance instance);

    void leaveDeploymentUnit(Instance instance);

}
