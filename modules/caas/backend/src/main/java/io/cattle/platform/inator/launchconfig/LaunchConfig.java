package io.cattle.platform.inator.launchconfig;

import io.cattle.platform.core.addon.DependsOn;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.InstanceWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;

import java.util.List;
import java.util.Map;

public interface LaunchConfig {

    String getName();

    Map<UnitRef, Unit> getDependencies();

    List<DependsOn> getDependsOn();

    InstanceWrapper create(InatorContext context, StackWrapper stack, DeploymentUnitWrapper unit);

    boolean validateDeps(InatorContext context, InstanceWrapper instanceWrapper);

    String getRevision();

    boolean isHealthcheckActionNone();

    boolean isStartFirst();

    String getImageUuid();

    Map<String, Object> getLabels();

    String getPullMode();

    Result applyDynamic(InstanceWrapper instance, InatorContext context);

    String getServiceName();

}
