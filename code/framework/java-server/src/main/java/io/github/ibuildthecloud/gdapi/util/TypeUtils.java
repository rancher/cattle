package io.github.ibuildthecloud.gdapi.util;

public class TypeUtils {

    public static final String ID_FIELD = "id";

    public static String guessPluralName(String name) {
        if (name == null) {
            return null;
        }

        if (name.endsWith("s") || name.endsWith("ch") || name.endsWith("x"))
            return name + "es";
        return name + "s";
    }

    public static String guessSingularName(String name) {
        if (name == null) {
            return null;
        }

        if (name.endsWith("ses") || name.endsWith("ches") || name.endsWith("xes"))
            return name.substring(0, name.length()-2);
        return name.substring(0, name.length()-1);
    }

}
