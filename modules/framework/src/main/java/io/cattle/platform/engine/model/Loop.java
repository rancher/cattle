package io.cattle.platform.engine.model;

public interface Loop {

    enum Result {
        WAITING, DONE
    }

    Result run(Object input);

}
