package io.cattle.platform.inator.deploy;

import io.cattle.platform.allocator.constraint.ContainerLabelAffinityConstraint;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.GenericObjectConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit.UnitState;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.launchconfig.LaunchConfig;
import io.cattle.platform.inator.wrapper.RevisionWrapper;
import io.cattle.platform.inator.wrapper.ServiceWrapper;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PullInator {

    ServiceWrapper service;
    InatorServices svc;

    public PullInator(ServiceWrapper service, InatorServices svc) {
        super();
        this.service = service;
        this.svc = svc;
    }

    public Result pull() {
        Map<String, LaunchConfig> changedLcs = getLaunchConfigsToBeUpgraded(service);
        Map<String, GenericObject> pullTasks = service.getPullTasks();
        Map<String, GenericObject> newPullTasks = new HashMap<>();
        List<String> waiting = new ArrayList<>();


        for (Map.Entry<String, LaunchConfig> entry : changedLcs.entrySet()) {
            String lcName = entry.getKey();
            LaunchConfig lc = entry.getValue();
            String imageUuid = lc.getImageUuid();
            String pullMode = lc.getPullMode();
            if (StringUtils.isBlank(imageUuid) || InstanceConstants.PULL_NONE.equals(pullMode) || StringUtils.isBlank(pullMode)) {
                continue;
            }

            GenericObject pullTask = pullTasks.get(imageUuid);
            if (pullTask == null) {
                pullTask = createPullTask(lcName, imageUuid, pullMode, lc);
            }

            newPullTasks.put(imageUuid, pullTask);

            if (!CommonStatesConstants.ACTIVE.equals(pullTask.getState()) && pullTask.getRemoved() == null) {
                waiting.add(imageUuid);
            } else {
                Map<String, Object> errors = DataAccessor.fieldMap(pullTask, "errors");
                if (errors.size() > 0) {
                    StringBuilder errMessage = new StringBuilder();
                    errors.forEach((k, v) -> errMessage.append(k).append(": ").append(v));
                    return new Result(UnitState.ERROR, null, String.format("Failed to pull %s: %s", imageUuid, errMessage));
                }
            }
        }

        service.savePullTasks(newPullTasks);

        if (waiting.size() > 0) {
            return new Result(UnitState.WAITING, null, String.format("Pulling image(s): %s", StringUtils.join(waiting, ", ")));
        }

        return Result.good();
    }

    private Map<String, LaunchConfig> getLaunchConfigsToBeUpgraded(ServiceWrapper service) {
        Map<String, LaunchConfig> result = new HashMap<>();
        Revision revision = svc.objectManager.loadResource(Revision.class, service.getRevisionId());
        Revision previousRevision = svc.objectManager.loadResource(Revision.class, service.getPreviousRevisionId());
        RevisionWrapper revisionWrapper = revision == null ? null : new RevisionWrapper(null, revision, null, svc);
        RevisionWrapper previousRevisionWrapper = previousRevision == null ? null : new RevisionWrapper(null,
                previousRevision, null, svc);

        if (revisionWrapper == null) {
            return result;
        }

        if (previousRevisionWrapper == null) {
            return revisionWrapper.getLaunchConfigs();
        }

        Map<String, LaunchConfig> oldLcs = previousRevisionWrapper.getLaunchConfigs();
        revisionWrapper.getLaunchConfigs().forEach((name, lc) -> {
            LaunchConfig oldLc = oldLcs.get(name);
            if (oldLc == null || !lc.getRevision().equals(oldLc.getRevision())) {
                result.put(name, lc);
            }
        });

        return result;
    }

    private GenericObject createPullTask(String lcName, String imageUuid, String pullMode, LaunchConfig lc) {
        Map<String, Object> labels = lc.getLabels();

        if (InstanceConstants.PULL_EXISTING.equals(pullMode)) {
            Stack stack = svc.objectManager.loadResource(Stack.class, service.getStackId());
            String name = stack.getName() + "/" + service.getName();
            if (!ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(lcName)) {
                name += "/" + lcName;
            }
            labels = CollectionUtils.asMap(ContainerLabelAffinityConstraint.LABEL_HEADER_AFFINITY_CONTAINER_LABEL,
                    SystemLabels.LABEL_STACK_SERVICE_NAME + "=" + name);
        }

        return svc.resourceDao.createAndSchedule(GenericObject.class,
                ObjectMetaDataManager.KIND_FIELD, GenericObjectConstants.KIND_PULL_TASK,
                ObjectMetaDataManager.ACCOUNT_FIELD, service.getAccountId(),
                ObjectMetaDataManager.NAME_FIELD, lcName,
                "image", imageUuid,
                InstanceConstants.FIELD_LABELS, labels,
                "serviceId", service.getId());
    }

}
