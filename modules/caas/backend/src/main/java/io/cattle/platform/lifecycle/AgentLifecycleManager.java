package io.cattle.platform.lifecycle;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;

public interface AgentLifecycleManager {

    ListenableFuture<Agent> create(Instance instance);

    void preRemove(Instance instance);

}
