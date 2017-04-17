package io.cattle.platform.inator.launchconfig;

import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.InstanceWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;

import java.util.Map;

public interface LaunchConfig {

    public String getName();

    public Map<UnitRef, Unit> getDependenciesHolders();

    public InstanceWrapper create(InatorContext context, StackWrapper stack, DeploymentUnitWrapper unit);

    public String getRevision();

    public boolean hasQuorum();

    public boolean isStartFirst();

}
