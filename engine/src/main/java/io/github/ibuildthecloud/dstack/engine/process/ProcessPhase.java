package io.github.ibuildthecloud.dstack.engine.process;

public enum ProcessPhase {
    /* The ordinal order of these is important */
    REQUESTED,
    STARTED,
    PRE_LISTENERS_DONE,
    HANDLER_DONE,
    POST_LISTENERS_DONE
}
