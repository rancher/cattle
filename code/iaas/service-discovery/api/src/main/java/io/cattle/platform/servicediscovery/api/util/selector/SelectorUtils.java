package io.cattle.platform.servicediscovery.api.util.selector;

import io.cattle.platform.servicediscovery.api.util.selector.SelectorConstraint.Op;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class SelectorUtils {
    private static Cache<String, List<SelectorConstraint<?>>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(3600, TimeUnit.SECONDS).build();

    public static boolean isSelectorMatch(String selector, Map<String, String> labels) {
        if (StringUtils.isEmpty(selector)) {
            return false;
        }
        List<SelectorConstraint<?>> constraints = getSelectorConstraints(selector);
        if (constraints.isEmpty()) {
            return false;
        }

        int found = 0;
        for (SelectorConstraint<?> constraint : constraints) {
            if (constraint.isMatch(labels)) {
                found++;
            }
        }
        if (found != constraints.size()) {
            return false;
        }
        return true;
    }

    public static List<SelectorConstraint<?>> getSelectorConstraints(String selector) {
        List<SelectorConstraint<?>> cachedConstraints = cache.getIfPresent(selector);
        if (cachedConstraints != null) {
            return cachedConstraints;
        }
        List<SelectorConstraint<?>> constraints = new ArrayList<>();
        List<String> selectorConstraints = new ArrayList<>();

        // as selector format is:
        // key in (value1, value2)
        // key notin (value1, value2)
        // key
        // key = value
        // key != value
        // and any combination of them can be specified in coma separated way:
        // key != value, key in (value1, value2), key = value

        boolean inList = false;
        StringBuffer selectorConstraint = new StringBuffer();
        for (int i = 0; i < selector.length(); i++) {
            boolean finishConstraint = (i == selector.length() - 1);
            char currentC = selector.charAt(i);
            if (currentC == '(') {
                inList = true;
            } else if (currentC == ')') {
                inList = false;
            } else if (currentC == ',') {
                if (inList) {
                    selectorConstraint.append(currentC);
                } else {
                    finishConstraint = true;
                }
            } else {
                selectorConstraint.append(currentC);
            }
            if (finishConstraint) {
                selectorConstraints.add(selectorConstraint.toString());
                selectorConstraint = new StringBuffer();
            }
        }

        for (String constraint : selectorConstraints) {
            SelectorConstraint<?> newSelectorConstraint = getSelectorConstraint(constraint);
            if (newSelectorConstraint.key.contains(" ")) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_FORMAT,
                        "Invalid format selector : " + selector);
            }
            constraints.add(newSelectorConstraint);
        }
        cache.put(selector, constraints);
        return constraints;
    }

    private static SelectorConstraint<?> getSelectorConstraint(String selector) {
        SelectorConstraint.Op finalOp = Op.NOOP;
        String key = StringUtils.EMPTY;
        String value = StringUtils.EMPTY;
        for (SelectorConstraint.Op op : SelectorConstraint.Op.values()) {
            if (op == Op.NOOP) {
                continue;
            }
            String[] exp = selector.split(op.getSelectorSymbol(), 2);
            if (exp.length == 2) {
                finalOp = op;
                key = exp[0].trim();
                value = exp[1].trim();
                break;
            }
        }
        if (finalOp == SelectorConstraint.Op.EQ) {
            return new SelectorConstraintEq(key, value);
        } else if (finalOp == SelectorConstraint.Op.NEQ) {
            return new SelectorConstraintNeq(key, value);
        } else if (finalOp == SelectorConstraint.Op.IN) {
            return new SelectorConstraintIn(key, splitValues(value));
        } else if (finalOp == SelectorConstraint.Op.NOTIN) {
            return new SelectorConstraintNotIn(key, splitValues(value));
        } else {
            return new SelectorConstraintNoop(selector.trim(), null);
        }
    }

    protected static List<String> splitValues(String value) {
        List<String> values = new ArrayList<>();
        for (String valueStr : value.split(",")) {
            values.add(valueStr.trim().toLowerCase());
        }
        return values;
    }
}
