package io.cattle.platform.process.common.util;

import io.cattle.platform.engine.handler.ProcessLogic;
import io.cattle.platform.util.type.ScopeUtils;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ProcessUtils {

    public static String getDefaultProcessName(Object obj) {
        String name = ScopeUtils.getScopeFromName(obj);
        String[] parts = name.split("[.]");
        if (parts.length > 2) {
            parts[parts.length - 1] = "." + parts[parts.length - 1];
            name = StringUtils.join(parts, "");
        }

        return name;
    }

    public static Map<String, Object> chainInData(Map<String, Object> data, String fromProcess, String toProcess) {
        data.put(fromProcess + ProcessLogic.CHAIN_PROCESS, toProcess);
        return data;
    }
}
