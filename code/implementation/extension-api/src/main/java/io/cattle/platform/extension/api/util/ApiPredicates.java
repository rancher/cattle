package io.cattle.platform.extension.api.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class ApiPredicates {

    private static final Logger log = LoggerFactory.getLogger(ApiPredicates.class);

    public static <T> Predicate<T> filterOn(Map<?, Object> criteria, String... keys) {
        Predicate<T> condition = Predicates.alwaysTrue();
        for (String prop : keys) {
            condition = Predicates.and(condition, ApiPredicates.fieldFilter(prop, criteria.get(prop)));
        }

        return condition;
    }

    public static final <T> Predicate<T> fieldFilter(final String name, final Object value) {
        return new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                if (value == null) {
                    return true;
                }
                Object result;
                try {
                    result = PropertyUtils.getProperty(input, name);
                } catch (IllegalAccessException e) {
                    log.error("Predicate check encountered an error for [{}] on [{}]", name, input, e);
                    return false;
                } catch (InvocationTargetException e) {
                    log.error("Predicate check encountered an error for [{}] on [{}]", name, input, e);
                    return false;
                } catch (NoSuchMethodException e) {
                    log.error("Predicate check encountered an error for [{}] on [{}]", name, input, e);
                    return false;
                }

                if (result == null) {
                    return false;
                }

                return value.toString().equals(result.toString());
            }
        };
    }
}
