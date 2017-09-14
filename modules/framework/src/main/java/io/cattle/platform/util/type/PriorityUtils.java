package io.cattle.platform.util.type;

public class PriorityUtils {

    public static int getPriority(Object obj) {
        if (obj instanceof Priority) {
            return ((Priority) obj).getPriority();
        } else if (obj instanceof String) {
            return getPriorityFromString((String) obj);
        }

        return Priority.SPECIFIC;
    }

    public static int getPriorityFromString(String value) {
        if ("SPECIFIC".equalsIgnoreCase(value)) {
            return Priority.SPECIFIC;
        }

        if ("PRE".equalsIgnoreCase(value)) {
            return Priority.PRE;
        }

        if ("DEFAULT".equalsIgnoreCase(value)) {
            return Priority.DEFAULT;
        }

        if ("DEFAULT_OVERRIDE".equalsIgnoreCase(value)) {
            return Priority.DEFAULT_OVERRIDE;
        }

        return Priority.SPECIFIC;
    }

}
