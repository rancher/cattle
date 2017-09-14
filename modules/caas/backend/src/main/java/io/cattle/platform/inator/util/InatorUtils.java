package io.cattle.platform.inator.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class InatorUtils {

    public static boolean objAndNumListEquals(Object left, Object right) {
        if (left instanceof List<?> && right instanceof List<?>) {
            return numListEquals((List<?>) left, (List<?>) right);
        } else if (left == null && right instanceof List<?>) {
            return numListEquals(Collections.emptyList(), (List<?>) right);
        } else if (left instanceof List<?> && right == null) {
            return numListEquals((List<?>) left, Collections.emptyList());
        }
        return Objects.equals(left, right);
    }

    protected static boolean numListEquals(List<?> left, List<?> right) {
        Set<Long> leftSet = new HashSet<>();
        Set<Long> rightSet = new HashSet<>();

        for (Object o : left) {
            if (o instanceof Number) {
                leftSet.add(((Number) o).longValue());
            } else {
                return false;
            }
        }

        for (Object o : right) {
            if (o instanceof Number) {
                rightSet.add(((Number) o).longValue());
            } else {
                return false;
            }
        }

        return leftSet.equals(rightSet);
    }

}
