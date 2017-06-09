package io.cattle.platform.engine.manager;

import com.google.common.util.concurrent.ListenableFuture;

public interface LoopManager {

    ListenableFuture<?> kick(String name, String type, Long id, Object input);

}
