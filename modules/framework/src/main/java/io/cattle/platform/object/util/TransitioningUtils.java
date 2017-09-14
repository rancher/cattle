package io.cattle.platform.object.util;

import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.cattle.platform.object.meta.ObjectMetaDataManager.*;
import static io.cattle.platform.object.util.ObjectUtils.*;

public class TransitioningUtils {

    public static Map<String, Object> getTransitioningErrorData(ExecutionException e) {
        if (e == null) {
            return Collections.emptyMap();
        }

        String message = e.getMessage();
        Map<String, Object> data = new HashMap<>();

        String finalMessage = message;
        if (StringUtils.isBlank(finalMessage)) {
            finalMessage = null;
        }

        data.put(ObjectMetaDataManager.TRANSITIONING_FIELD, finalMessage == null ? null : ObjectMetaDataManager.TRANSITIONING_ERROR);
        data.put(ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD, finalMessage);

        return data;
    }

    public static Map<String, Object> getTransitioningErrorData(Object obj) {
        String state = getState(obj);
        String error = DataAccessor.fieldString(obj, TRANSITIONING_FIELD);
        String message = DataAccessor.fieldString(obj, TRANSITIONING_MESSAGE_FIELD);

        if (TRANSITIONING_ERROR.equals(error) || TRANSITIONING_ERROR.equals(state)) {
            return CollectionUtils.asMap(TRANSITIONING_FIELD, error, TRANSITIONING_MESSAGE_FIELD, message);
        }

        return Collections.emptyMap();
    }

    public static String getTransitioningErrorMessage(Object obj) {
        return Objects.toString(getTransitioningErrorData(obj).get(TRANSITIONING_MESSAGE_FIELD), null);
    }

}