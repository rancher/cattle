package io.cattle.platform.engine.process;

public enum ProcessPhase {
    /* The ordinal order of these is important */
    REQUESTED, STARTED, PRE_LISTENERS, PRE_LISTENERS_DONE, HANDLERS, HANDLER_DONE, POST_LISTENERS, POST_LISTENERS_DONE, DONE
}
