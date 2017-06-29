package io.cattle.platform.util.type;

public interface Priority {

    public static final int PRE = 500;
    public static final int SPECIFIC = 1000;
    public static final int DEFAULT_OVERRIDE = 1500;
    public static final int DEFAULT = 2000;

    int getPriority();

}
