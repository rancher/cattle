package io.github.ibuildthecloud.dstack.util.type;

public class ScopeUtils {

    public static final String getDefaultScope(Object obj) {
        if ( obj instanceof Scope ) {
            return ((Scope)obj).getDefaultScope();
        }

        return "";
    }
}
