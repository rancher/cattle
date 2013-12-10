package io.github.ibuildthecloud.dstack.extension;

import java.util.List;

public interface ExtensionManager {

    <T> List<T> getExtensionList(String key, Class<T> type);

//    <T> Map<String, List<T>> getExtensionMap(String key, Class<T> type);

    void onChange(String key, Runnable runnable);

}
