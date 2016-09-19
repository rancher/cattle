package io.github.ibuildthecloud.gdapi.id;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IdFormatterUtils {

    public static Object formatReference(Field field, IdFormatter formatter, Object value, SchemaFactory schemaFactory) {
        return formatReference(field, field.getTypeEnum(), field.getSubTypeEnums(), field.getSubTypes(), formatter, value, schemaFactory);
    }

    private static Object formatReference(Field field, FieldType fieldType, List<FieldType> subTypeEnums,
            List<String> subTypes, IdFormatter formatter, Object value, SchemaFactory schemaFactory) {
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
        case TYPE:
            if (field == null || schemaFactory == null) {
                return value;
            }
            return formatType(field, subTypeEnums, subTypes, formatter, value, schemaFactory);
        default:
            return value;
        }
    }


    private static Object formatType(Field field, List<FieldType> subTypeEnums, List<String> subTypes,
            IdFormatter formatter, Object value, SchemaFactory schemaFactory) {
        if (value == null || !(value instanceof Map)) {
            return value;
        }

        if(schemaFactory == null) {
            return value;
        }

        Map<?, ?> inputs = (Map<?, ?>)value;
        Map<Object, Object> result = new LinkedHashMap<Object, Object>();

        Schema fieldSchema = schemaFactory.getSchema(field.getType());
        if (!result.containsKey("type")) {
            result.put("type", fieldSchema.getId());
        }

        for (Map.Entry<String, Field> entry : fieldSchema.getResourceFields().entrySet()) {
            String fieldName = entry.getKey();
            if (inputs.containsKey(fieldName)) {
                Object subFieldValue = inputs.get(fieldName);

                if (subFieldValue != null) {
                    Field subField = entry.getValue();
                    Object formattedValue = formatReference(subField, subField.getTypeEnum(), subField.getSubTypeEnums(),
                            subField.getSubTypes(), formatter, subFieldValue, schemaFactory);
                    result.put(fieldName, formattedValue);
                } else {
                    result.put(fieldName, subFieldValue);
                }
            }
        }

        return result;
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
            result.add(formatReference(null, fieldType, subTypeEnums, subTypes, formatter, input, null));
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
            result.put(entry.getKey(), formatReference(null, fieldType, subTypeEnums, subTypes, formatter, entry.getValue(), null));
        }

        return result;
    }
}
