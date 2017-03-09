package io.cattle.platform.servicediscovery.api.service;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.ServiceRevision;
import io.cattle.platform.core.model.Stack;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public interface ServiceDataManager {

    Map<String, Object> getDeploymentUnitInstanceData(Stack stack, Service service, DeploymentUnit unit,
            String launchConfigName, ServiceIndex serviceIndex, String instanceName);

    void joinService(Instance instance, Map<String, Object> instanceLaunchConfig);

    void leaveService(Instance instance);

    Map<String, List<String>> getUsedBySidekicks(Service service);

    List<String> getLaunchConfigSidekickReferences(Service service, String launchConfigName);

    ServiceRevision createRevision(Service service, Map<String, Object> primaryLaunchConfig,
            List<Map<String, Object>> secondaryLaunchConfigs, boolean isFirstRevision);

    void cleanupServiceRevisions(Service service);

    Pair<ServiceRevision, ServiceRevision> getCurrentAndPreviousRevisions(Service service);

    ServiceRevision getCurrentRevision(Service service);

    Pair<Map<String, Object>, List<Map<String, Object>>> getPrimaryAndSecondaryConfigFromRevision(
            ServiceRevision revision, Service service);

    void createInitialServiceRevision(Service service);

    List<String> getServiceLaunchConfigNames(Service service);

}
