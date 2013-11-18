package io.github.ibuildthecloud.dstack.object.postinit;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

public class SpecialFieldsPostInstantiationHandler implements ObjectPostInstantiationHandler {

    public static final String UUID = "uuid";
    public static final String ACCOUNT_ID = "accountId";
    public static final String ZONE_ID = "zoneId";
    public static final String CREATED = "created";
    public static final String STATE = "state";

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties) {
        set(obj, UUID, java.util.UUID.randomUUID().toString());
        set(obj, CREATED, new Date());
        set(obj, STATE, "requested");

        //TODO Bad!
        set(obj, ACCOUNT_ID, 1L);
        set(obj, ZONE_ID, 1L);
        return obj;
    }

    protected void set(Object obj, String property, Object value) {
        try {
            BeanUtils.setProperty(obj, property, value);
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
