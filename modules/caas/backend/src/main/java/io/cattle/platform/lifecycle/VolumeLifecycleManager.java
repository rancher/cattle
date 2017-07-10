package io.cattle.platform.lifecycle;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.lifecycle.util.LifecycleException;

import java.util.Arrays;
import java.util.List;

public interface VolumeLifecycleManager {

    List<Object> MOUNT_STATES = Arrays.asList(
            CommonStatesConstants.INACTIVE,
            CommonStatesConstants.DEACTIVATING,
            CommonStatesConstants.REMOVED,
            CommonStatesConstants.REMOVING);

    void create(Instance instance) throws LifecycleException;

    void preStart(Instance instance) throws LifecycleException;

    void preRemove(Instance instance);

}
