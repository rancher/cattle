package io.github.ibuildthecloud.gdapi.id;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class TypeIdFormatter implements IdFormatter {

    private Map<String, String> typeCache = Collections.synchronizedMap(new WeakHashMap<String, String>());

    String globalPrefix = "1";
    SchemaFactory schemaFactory;
    Set<String> plainTypes = new HashSet<String>();
    Map<String, String> typeMappings = new HashMap<>();

    protected TypeIdFormatter() {
    }

    @Override
    public String formatId(String type, Object id) {
        if (id == null) {
            return null;
        }

        String idString = id.toString();
        if (idString.length() == 0) {
            return null;
        }

        if (plainTypes.contains(type)) {
            return id.toString();
        }

        String shortType = typeCache.get(type);
        if (shortType == null) {
            shortType = getShortType(type);
        }

        if (!Character.isDigit(idString.charAt(0))) {
            return shortType + "!" + id;
        } else {
            return shortType + id;
        }
    }

    @Override
    public String parseId(String id) {
        if (id == null || id.length() == 0)
            return null;

        if (Character.isLetter(id.charAt(0)) && !id.startsWith(globalPrefix)) {
            return id;
        }

        if (!id.startsWith(globalPrefix)) {
            return null;
        }

        id = id.substring(globalPrefix.length());

        if (id.length() == 0 || !Character.isLetter(id.charAt(0))) {
            return null;
        }

        String parsedId = id.replaceAll("^[a-z]*", "");
        try {
            if (parsedId.startsWith("!")) {
                return parsedId.substring(1);
            } else {
                return parsedId;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public IdFormatter withSchemaFactory(SchemaFactory schemaFactory) {
        TypeIdFormatter formatter = newFormatter();
        formatter.schemaFactory = schemaFactory;
        formatter.globalPrefix = this.globalPrefix;
        formatter.plainTypes = this.plainTypes;
        formatter.typeMappings = this.typeMappings;
        return formatter;
    }

    protected TypeIdFormatter newFormatter() {
        return new TypeIdFormatter();
    }

    protected String getShortType(String type) {
        String base = schemaFactory.getBaseType(type);
        if (base != null) {
            type = base;
        }

        StringBuilder buffer = new StringBuilder(globalPrefix);
        String mapping = typeMappings.get(type);
        if (mapping == null) {
            buffer.append(type.charAt(0));
            buffer.append(type.replaceAll("[a-z]+", ""));
        } else {
            buffer.append(mapping);
        }

        String result = buffer.toString().toLowerCase();
        typeCache.put(type, result);

        return result;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public Set<String> getPlainTypes() {
        return plainTypes;
    }

    public void setPlainTypes(Set<String> plainTypes) {
        this.plainTypes = plainTypes;
    }

    public Map<String, String> getTypeMappings() {
        return typeMappings;
    }

    public void setTypeMappings(Map<String, String> typeMappings) {
        this.typeMappings = typeMappings;
    }

}
