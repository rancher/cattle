package io.github.ibuildthecloud.dstack.util.type;

public class PriorityUtils {

    public static int getPriority(Object obj) {
        if ( obj instanceof Priority ) {
            return ((Priority)obj).getPriority();
        }

        return Priority.SPECIFIC;
    }
}
