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
        return formatReference(field.getTypeEnum(), field.getType(), field.getSubTypeEnums(), field.getSubTypes(), formatter,
                value, schemaFactory);
    }

    private static Object formatReference(FieldType fieldType, String schemaType, List<FieldType> subTypeEnums,
            List<String> subTypes, IdFormatter formatter, Object value, SchemaFactory schemaFactory) {
        if (value == null) {
            return value;
        }

        switch (fieldType) {
        case REFERENCE:
            String type = subTypes.get(0);
            return formatter.formatId(type, value);
        case ARRAY:
            return formatList(subTypeEnums, subTypes, formatter, value, schemaFactory);
        case MAP:
            return formatMap(subTypeEnums, subTypes, formatter, value,  schemaFactory);
        case TYPE:
            if (schemaType == null || schemaFactory == null) {
                return value;
            }
            return formatType(formatter, value, schemaFactory, schemaType);
        default:
            return value;
        }
    }


    private static Object formatType(IdFormatter formatter, Object value, SchemaFactory schemaFactory,
            String schemaType) {
        if (value == null || !(value instanceof Map)) {
            return value;
        }

        if (schemaFactory == null || schemaType == null) {
            return value;
        }

        Map<?, ?> inputs = (Map<?, ?>)value;
        Map<Object, Object> result = new LinkedHashMap<Object, Object>();

        Schema fieldSchema = schemaFactory.getSchema(schemaType);
        if (!result.containsKey("type")) {
            result.put("type", fieldSchema.getId());
        }

        for (Map.Entry<String, Field> entry : fieldSchema.getResourceFields().entrySet()) {
            String fieldName = entry.getKey();
            if (inputs.containsKey(fieldName)) {
                Object subFieldValue = inputs.get(fieldName);

                if (subFieldValue != null) {
                    Field subField = entry.getValue();
                    Object formattedValue = formatReference(subField.getTypeEnum(), subField.getType(), subField.getSubTypeEnums(),
                            subField.getSubTypes(), formatter, subFieldValue, schemaFactory);
                    result.put(fieldName, formattedValue);
                } else {
                    result.put(fieldName, subFieldValue);
                }
            }
        }

        return result;
    }

    private static Object formatList(List<FieldType> subTypeEnums, List<String> subTypes, IdFormatter formatter, Object value, SchemaFactory schemaFactory) {
        if (value == null || subTypeEnums.size() == 0) {
            return value;
        }

        if (!(value instanceof List)) {
            return value;
        }

        List<?> inputs = (List<?>)value;
        List<Object> result = new ArrayList<Object>(inputs.size());
        FieldType fieldType = subTypeEnums.get(0);

        String schemaType = null;
        if (subTypes.size() > 1) {
            schemaType = subTypes.get(1);
        } else {
            schemaType = subTypes.get(0);
        }

        subTypeEnums = subTypeEnums.subList(1, subTypeEnums.size());
        subTypes = subTypes.subList(1, subTypes.size());
        for (Object input : inputs) {
            result.add(formatReference(fieldType, schemaType, subTypeEnums, subTypes, formatter, input,
                    schemaFactory));
        }

        return result;
    }

    private static Object formatMap(List<FieldType> subTypeEnums, List<String> subTypes, IdFormatter formatter, Object value, SchemaFactory schemaFactory) {
        if (value == null || subTypeEnums.size() == 0) {
            return value;
        }

        if (!(value instanceof Map)) {
            return value;
        }

        Map<?, ?> inputs = (Map<?, ?>)value;
        Map<Object, Object> result = new LinkedHashMap<Object, Object>();
        FieldType fieldType = subTypeEnums.get(0);
        String schemaType = null;
        if (subTypes.size() > 1) {
            schemaType = subTypes.get(1);
        } else {
            schemaType = subTypes.get(0);
        }

        subTypeEnums = subTypeEnums.subList(1, subTypeEnums.size());
        subTypes = subTypes.subList(1, subTypes.size());

        for (Map.Entry<?, ?> entry : inputs.entrySet()) {
            result.put(entry.getKey(),
                    formatReference(fieldType, schemaType, subTypeEnums, subTypes, formatter, entry.getValue(),
                            schemaFactory));
        }

        return result;
    }
}
