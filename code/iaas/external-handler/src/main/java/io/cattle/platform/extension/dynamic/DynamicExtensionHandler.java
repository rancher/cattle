package io.cattle.platform.extension.dynamic;

import java.util.List;

public interface DynamicExtensionHandler {

    <T> List<T> getExtensionList(String key, Class<T> type);

}
