package io.github.ibuildthecloud.dstack.util.type;

public class ScopeUtils {

    public static final String getDefaultScope(Object obj) {
        if ( obj instanceof Scope ) {
            return ((Scope)obj).getDefaultScope();
        }

        return "";
    }

    public static final String getScopeFromName(Object obj) {
        return NamedUtils.getName(obj).replaceAll("([a-z])([A-Z])", "$1.$2").toLowerCase();
    }

    public static final String getScopeFromClass(Class<?> clz) {
        return clz.getSimpleName().replaceAll("([a-z])([A-Z])", "$1.$2").toLowerCase();
    }
}
