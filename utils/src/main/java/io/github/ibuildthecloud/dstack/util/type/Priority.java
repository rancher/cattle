package io.github.ibuildthecloud.dstack.util.type;

public interface Priority {

    public static final int PRE = 500;
    public static final int SPECIFIC = 1000;
    public static final int DEFAULT = 1500;

    int getPriority();

}
