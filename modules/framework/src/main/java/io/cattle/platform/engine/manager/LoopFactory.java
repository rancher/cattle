package io.cattle.platform.engine.manager;

import io.cattle.platform.engine.model.Loop;

public interface LoopFactory {

    Loop buildLoop(String name, String type, Long id);

}
