package io.github.ibuildthecloud.gdapi.model;

import static io.github.ibuildthecloud.gdapi.condition.ConditionType.*;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

public enum FieldType {
    /*
     * This order is important, refer to SchemaFactoryImpl.assignSimpleType()
     */
    STRING(STRING_MODS, String.class), PASSWORD(String.class), FLOAT(NUMBER_MODS, Float.class, Float.TYPE, Double.class, Double.TYPE), INT(NUMBER_MODS,
            Long.class, Long.TYPE, Integer.class, Integer.TYPE), DATE(NUMBER_MODS, Date.class), BLOB(InputStream.class), BOOLEAN(VALUE_MODS, Boolean.class,
            Boolean.TYPE), ENUM(VALUE_MODS, String.class), REFERENCE(VALUE_MODS, IdRef.class), ARRAY(List.class, Object[].class), MAP(Map.class), TYPE(
            Object.class), JSON(Object.class), NONE;

    Class<?>[] clzs;
    Set<ConditionType> modifiers = new TreeSet<ConditionType>();

    private FieldType(ConditionType[] modifiers, Class<?>... clzs) {
        this.clzs = clzs;
        for (ConditionType mod : modifiers) {
            this.modifiers.add(mod);
        }
    }

    private FieldType(Class<?>... clzs) {
        this.clzs = clzs;
    }

    public static String toString(FieldType fieldType, String... list) {
        return toString(null, fieldType, list);
    }

    public static String toString(String name, FieldType fieldType, String... list) {
        return toString(name, fieldType, Arrays.asList(list));
    }

    public static String toString(String name, FieldType fieldType, List<String> list) {
        StringBuilder buffer = new StringBuilder();
        if (name == null) {
            if (fieldType != null) {
                buffer.append(fieldType.getExternalType());
            }
        } else {
            buffer.append(name);
        }

        int count = 0;
        for (String item : list) {
            if (buffer.length() > 0) {
                count++;
                buffer.append("[");
            }
            buffer.append(item);
        }

        for (int i = 0; i < count; i++) {
            buffer.append("]");
        }

        return buffer.toString();
    }

    public static List<TypeAndName> parse(String typeName) {
        List<TypeAndName> result = new ArrayList<TypeAndName>();
        String[] parts = typeName.split("\\[");

        for (int i = 0; i < parts.length; i++) {
            String part = StringUtils.stripEnd(parts[i], "]").trim();
            TypeAndName typeAndName = new TypeAndName();
            typeAndName.name = part;
            try {
                typeAndName.type = FieldType.valueOf(part.toUpperCase());
            } catch (IllegalArgumentException e) {
                if (i != parts.length - 1) {
                    throw new IllegalArgumentException("Invalid type [" + typeName + "]", e);
                }
                typeAndName.type = FieldType.TYPE;
            }
            result.add(typeAndName);
        }

        return result;
    }

    public Class<?>[] getClasses() {
        return clzs;
    }

    public Set<ConditionType> getModifiers() {
        return this.modifiers;
    }

    public String getExternalType() {
        return toString().toLowerCase();
    }

    public static class TypeAndName {
        FieldType type;
        String name;

        public TypeAndName() {
        }

        public TypeAndName(FieldType type, String name) {
            super();
            this.type = type;
            this.name = name;
        }

        public FieldType getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }
}
