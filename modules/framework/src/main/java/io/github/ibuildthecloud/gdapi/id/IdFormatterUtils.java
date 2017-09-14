package io.github.ibuildthecloud.gdapi.id;

import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IdFormatterUtils {

    private static final Logger log = LoggerFactory.getLogger(IdFormatter.class);

    public static Object formatReference(boolean dropNull, Field field, IdFormatter formatter, Object value, SchemaFactory schemaFactory) {
        return formatReference(dropNull, field.getTypeEnum(), field.getType(), field.getSubTypeEnums(), field.getSubTypes(), formatter,
                value, schemaFactory);
    }

    private static Object formatReference(boolean dropNull, FieldType fieldType, String schemaType, List<FieldType> subTypeEnums,
            List<String> subTypes, IdFormatter formatter, Object value, SchemaFactory schemaFactory) {
        if (value == null) {
            return value;
        }

        switch (fieldType) {
        case REFERENCE:
            String type = subTypes.get(0);
            return formatter.formatId(type, value);
        case ARRAY:
            return formatList(dropNull, subTypeEnums, subTypes, formatter, value, schemaFactory);
        case MAP:
            return formatMap(dropNull, subTypeEnums, subTypes, formatter, value,  schemaFactory);
        case TYPE:
            if (schemaType == null || schemaFactory == null) {
                return value;
            }
            return formatType(dropNull, formatter, value, schemaFactory, schemaType);
        default:
            return value;
        }
    }

    private static Schema getMostSpecific(SchemaFactory schemaFactory, Schema left, Schema right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        while (left != null && left.getParent() != null) {
            if (right.getId().equals(left.getParent())) {
                return left;
            }
            left = schemaFactory.getSchema(left.getParent());
        }
        return right;
    }

    private static Object formatType(boolean dropNull, IdFormatter formatter, Object value, SchemaFactory schemaFactory,
            String schemaType) {
        if (value == null) {
            return value;
        }

        if (schemaFactory == null || schemaType == null) {
            return value;
        }

        Map<Object, Object> result = new LinkedHashMap<>();

        Schema fieldSchema = getMostSpecific(schemaFactory,
                schemaFactory.getSchema(schemaType), schemaFactory.getSchema(value.getClass()));
        if (fieldSchema == null) {
            log.error("Failed to find schema for type [{}]", schemaType);
            return result;
        }

        if (!result.containsKey("type")) {
            result.put("type", fieldSchema.getId());
        }

        for (Map.Entry<String, Field> entry : fieldSchema.getResourceFields().entrySet()) {
            String fieldName = entry.getKey();
            Object subFieldValue;
            if (value instanceof Map<?, ?>) {
                if (((Map<?, ?>)value).containsKey(fieldName)) {
                    subFieldValue = ((Map<?, ?>) value).get(fieldName);
                } else {
                    continue;
                }
            } else {
                subFieldValue = entry.getValue().getValue(value);
                if (subFieldValue == null) {
                    subFieldValue = DataAccessor.fields(value).withKey(fieldName).get();
                }
            }

            if (subFieldValue != null) {
                Field subField = entry.getValue();
                Object formattedValue = null;
                if (TypeUtils.ID_FIELD.equals(subField.getName())) {
                    formattedValue = formatReference(dropNull, FieldType.REFERENCE, null, null,
                            Collections.singletonList(schemaType), formatter, subFieldValue, schemaFactory);
                } else {
                    formattedValue = formatReference(dropNull, subField.getTypeEnum(), subField.getType(), subField.getSubTypeEnums(),
                            subField.getSubTypes(), formatter, subFieldValue, schemaFactory);
                }
                addValue(dropNull, result, fieldName, formattedValue);
            } else {
                addValue(dropNull, result, fieldName, subFieldValue);
            }
        }

        return result;
    }

    private static void addValue(boolean dropNull, Map<Object, Object> map, String fieldName, Object value) {
        if (value == null && dropNull) {
            return;
        }
        map.put(fieldName, value);
    }

    private static Object formatList(boolean dropNull, List<FieldType> subTypeEnums, List<String> subTypes, IdFormatter formatter, Object value, SchemaFactory schemaFactory) {
        if (value == null || subTypeEnums.size() == 0) {
            return value;
        }

        if (!(value instanceof List) && !(value instanceof Set)) {
            return value;
        }

        Collection<?> inputs = (Collection<?>)value;
        List<Object> result = new ArrayList<>(inputs.size());
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
            result.add(formatReference(dropNull, fieldType, schemaType, subTypeEnums, subTypes, formatter, input,
                    schemaFactory));
        }

        return result;
    }

    private static Object formatMap(boolean dropNull, List<FieldType> subTypeEnums, List<String> subTypes, IdFormatter formatter, Object value, SchemaFactory schemaFactory) {
        if (value == null || subTypeEnums.size() == 0) {
            return value;
        }

        if (!(value instanceof Map)) {
            return value;
        }

        Map<?, ?> inputs = (Map<?, ?>)value;
        Map<Object, Object> result = new LinkedHashMap<>();
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
                    formatReference(dropNull, fieldType, schemaType, subTypeEnums, subTypes, formatter, entry.getValue(),
                            schemaFactory));
        }

        return result;
    }
}
