package io.cattle.platform.db.jooq.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.lang3.reflect.ConstructorUtils;

public class AggregateMultiRecordMapper<T> extends MultiRecordMapper<T> {

    Class<T> resultType;
    Constructor<T> ctor;

    public AggregateMultiRecordMapper(Class<T> resultType) {
        super();
        /*
         * I could probably find this out with reflection magic, too lazy, maybe
         * slow?
         */
        this.resultType = resultType;
    }

    @Override
    protected T map(List<Object> input) {
        if (ctor == null) {
            try {
                ctor = ConstructorUtils.getMatchingAccessibleConstructor(resultType, classes.toArray(new Class<?>[classes.size()]));
            } catch (SecurityException e) {
                throw new IllegalArgumentException("Failed to find constructor", e);
            }
        }

        try {
            return ctor.newInstance(input.toArray(new Object[input.size()]));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to construct object", e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Failed to construct object", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to construct object", e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to construct object", e);
        }
    }

}
