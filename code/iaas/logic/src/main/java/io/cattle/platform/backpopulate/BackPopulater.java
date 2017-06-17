package io.cattle.platform.backpopulate;

import io.cattle.platform.core.model.Instance;

public interface BackPopulater {

    void update(Instance instance);

    int getExitCode(Instance instance);

}
