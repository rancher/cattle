package io.cattle.platform.inator.launchconfig;

import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.InstanceWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;

import java.util.Map;

public interface LaunchConfig {

    String getName();

    Map<UnitRef, Unit> getDependencies();

    InstanceWrapper create(InatorContext context, StackWrapper stack, DeploymentUnitWrapper unit);

    boolean validateDeps(InatorContext context, InstanceWrapper instanceWrapper);

    String getRevision();

    boolean isHealthcheckActionNone();

    boolean isStartFirst();

    String getImageUuid();

    Map<String, Object> getLabels();

    String getPullMode();

    void applyDynamic(InstanceWrapper instance, InatorContext context);

    String getServiceName();
}
