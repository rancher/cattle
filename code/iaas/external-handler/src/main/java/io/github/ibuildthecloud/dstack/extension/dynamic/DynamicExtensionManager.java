package io.github.ibuildthecloud.dstack.extension.dynamic;

import io.github.ibuildthecloud.dstack.extension.impl.ExtensionManagerImpl;
import io.github.ibuildthecloud.dstack.util.type.PriorityUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DynamicExtensionManager extends ExtensionManagerImpl {

    private static final String DYNAMIC_HANDLER_KEY = "dynamic.extension.handler";

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getExtensionList(String key, Class<T> type) {
        List<T> extensions = super.getExtensionList(key, type);
        if ( type == null ) {
            type = (Class<T>)getExpectedType(key);
        }

        if ( DYNAMIC_HANDLER_KEY.equals(key) || DynamicExtensionHandler.class == type ) {
            return extensions;
        }

        for ( DynamicExtensionHandler handler : getExtensionList(DynamicExtensionHandler.class) ) {
            List<T> additional = handler.getExtensionList(key, type);

            if ( additional.size() == 0 ) {
                continue;
            }

            List<Object> merged = new ArrayList<Object>(extensions.size() + additional.size());
            Iterator<T> iter = additional.iterator();
            T current = iter.next();

            for ( Object obj : extensions ) {
                while ( current != null && PriorityUtils.getPriority(obj) > PriorityUtils.getPriority(current) ) {
                    merged.add(current);
                    if ( iter.hasNext() ) {
                        current = iter.next();
                    } else {
                        current = null;
                    }
                }
                merged.add(obj);
            }

            if ( current != null ) {
                merged.add(current);
            }

            while ( iter.hasNext() ) {
                merged.add(iter.next());
            }

            return (List<T>)merged;
        }

        return extensions;
    }

}
