package io.github.ibuildthecloud.dstack.object;

import java.util.Map;

public interface ObjectDefaultsProvider {

    Map<? extends Class<?>, ? extends Map<String, Object>> getDefaults();

}
