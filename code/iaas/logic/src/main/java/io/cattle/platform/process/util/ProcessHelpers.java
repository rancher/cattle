package io.cattle.platform.process.util;

import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessHelpers {

    public static <T> List<T> createOneChild(ObjectManager objectManager, ObjectProcessManager processManager, Object parent, Class<T> childClass, Object key,
            Object... values) {

        List<T> children = objectManager.children(parent, childClass);
        if (children.size() == 0) {
            children = Arrays.asList(objectManager.create(childClass, key, values));
        }

        for (T child : children) {
            processManager.executeStandardProcess(StandardProcess.CREATE, child, null);
        }

        return children;
    }

    public static <T> List<T> getNonRemovedChildren(ObjectManager objectManager, Object obj, Class<T> type) {
        List<T> result = new ArrayList<>();

        for (T child : objectManager.children(obj, type)) {
            if (ObjectUtils.getRemoved(child) == null) {
                result.add(child);
            }
        }

        return result;
    }

}
