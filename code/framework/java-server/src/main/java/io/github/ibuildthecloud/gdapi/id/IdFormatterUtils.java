package io.github.ibuildthecloud.gdapi.id;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IdFormatterUtils {

    public static IdFormatter getFormatter(IdFormatter idformatter) {
        if (ApiContext.getContext() !=  null && ApiContext.getContext().getIdFormatter() != null) {
            return ApiContext.getContext().getIdFormatter();
        }
        return idformatter;
    }
    public static Object formatReference(Field field, IdFormatter formatter, Object value) {
        return formatReference(field.getTypeEnum(), field.getSubTypeEnums(), field.getSubTypes(), formatter, value);
    }

    private static Object formatReference(FieldType fieldType, List<FieldType> subTypeEnums, List<String> subTypes, IdFormatter formatter, Object value) {
        if (value == null) {
            return value;
        }

        switch (fieldType) {
        case REFERENCE:
            String type = subTypes.get(0);
            return formatter.formatId(type, value);
        case ARRAY:
            return formatList(subTypeEnums, subTypes, formatter, value);
        case MAP:
            return formatMap(subTypeEnums, subTypes, formatter, value);
        default:
            return value;
        }
    }

    private static Object formatList(List<FieldType> subTypeEnums, List<String> subTypes, IdFormatter formatter, Object value) {
        if (value == null || subTypeEnums.size() == 0) {
            return value;
        }

        if (!(value instanceof List)) {
            return value;
        }

        List<?> inputs = (List<?>)value;
        List<Object> result = new ArrayList<Object>(inputs.size());
        FieldType fieldType = subTypeEnums.get(0);
        subTypeEnums = subTypeEnums.subList(1, subTypeEnums.size());
        subTypes = subTypes.subList(1, subTypes.size());

        for (Object input : inputs) {
            result.add(formatReference(fieldType, subTypeEnums, subTypes, formatter, input));
        }

        return result;
    }

    private static Object formatMap(List<FieldType> subTypeEnums, List<String> subTypes, IdFormatter formatter, Object value) {
        if (value == null || subTypeEnums.size() == 0) {
            return value;
        }

        if (!(value instanceof Map)) {
            return value;
        }

        Map<?, ?> inputs = (Map<?, ?>)value;
        Map<Object, Object> result = new LinkedHashMap<Object, Object>();
        FieldType fieldType = subTypeEnums.get(0);
        subTypeEnums = subTypeEnums.subList(1, subTypeEnums.size());
        subTypes = subTypes.subList(1, subTypes.size());

        for (Map.Entry<?, ?> entry : inputs.entrySet()) {
            result.put(entry.getKey(), formatReference(fieldType, subTypeEnums, subTypes, formatter, entry.getValue()));
        }

        return result;
    }
}
