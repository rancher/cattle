package io.cattle.platform.engine.model;

import java.util.List;

public interface Loop {

    enum Result {
        WAITING, DONE
    }

    Result run(List<Object> input);

}
