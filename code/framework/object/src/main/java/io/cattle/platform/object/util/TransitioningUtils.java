package io.cattle.platform.object.util;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.exception.ExecutionException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicBooleanProperty;

public class TransitioningUtils {

    public static final DynamicBooleanProperty SHOW_INTERNAL_MESSAGES =
            ArchaiusUtil.getBoolean("api.show.transitioning.internal.message");

    public static Map<String,Object> getTransitioningData(ExecutionException e) {
        if ( e == null ) {
            return Collections.emptyMap();
        }

        return getTransitioningData(e.getTransitioningMessage(), e.getTransitioningInternalMessage());
    }

    public static Map<String,Object> getTransitioningData(String message, String internalMessage) {
        Map<String,Object> data = new HashMap<String, Object>();

        String finalMessage = message == null ? "" : message;

        if ( SHOW_INTERNAL_MESSAGES.get() && ! StringUtils.isBlank(internalMessage) ) {
                finalMessage = finalMessage + " : " + internalMessage;
        }

        if ( StringUtils.isBlank(finalMessage) ) {
            finalMessage = null;
        }

        data.put(ObjectMetaDataManager.TRANSITIONING_FIELD,
                finalMessage == null ? null : ObjectMetaDataManager.TRANSITIONING_ERROR);
        data.put(ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD,
                finalMessage);

        return data;
    }

}