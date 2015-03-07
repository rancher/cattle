package io.cattle.platform.util.type;

public class ScopeUtils {

    public static final String getDefaultScope(Object obj) {
        if (obj instanceof Scope) {
            return ((Scope) obj).getTargetScope();
        }

        return "";
    }

    public static final String getScopeFromName(Object obj) {
        return NamedUtils.toDotSeparated(NamedUtils.getName(obj));
    }

    public static final String getScopeFromClass(Class<?> clz) {
        return NamedUtils.toDotSeparated(clz.getSimpleName());
    }
}
