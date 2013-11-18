package io.github.ibuildthecloud.dstack.object.postinit;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

public class SetPropertiesPostInstantiationHandler implements ObjectPostInstantiationHandler {

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties) {
        try {
            BeanUtils.copyProperties(obj, properties);
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return obj;
    }

}
