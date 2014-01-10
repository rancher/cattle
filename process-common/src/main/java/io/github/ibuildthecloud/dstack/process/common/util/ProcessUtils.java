package io.github.ibuildthecloud.dstack.process.common.util;

import io.github.ibuildthecloud.dstack.util.type.ScopeUtils;

import org.apache.commons.lang3.StringUtils;

public class ProcessUtils {

    public static String getDefaultProcessName(Object obj) {
        String name = ScopeUtils.getScopeFromName(obj);
        String[] parts = name.split("[.]");
        if ( parts.length > 2 ) {
            parts[parts.length-1] = "." + parts[parts.length-1];
            name = StringUtils.join(parts, "");
        }

        return name;
    }

}
