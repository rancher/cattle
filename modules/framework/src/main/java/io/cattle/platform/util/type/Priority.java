package io.cattle.platform.util.type;

public interface Priority {

    int PRE = 500;
    int SPECIFIC = 1000;
    int DEFAULT_OVERRIDE = 1500;
    int DEFAULT = 2000;

    int getPriority();

}
