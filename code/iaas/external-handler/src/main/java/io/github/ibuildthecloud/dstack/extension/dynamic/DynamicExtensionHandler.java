package io.github.ibuildthecloud.dstack.extension.dynamic;

import java.util.List;

public interface DynamicExtensionHandler {

    <T> List<T> getExtensionList(String key, Class<T> type);

}
