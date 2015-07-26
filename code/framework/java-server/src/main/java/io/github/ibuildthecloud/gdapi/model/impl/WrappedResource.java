package io.github.ibuildthecloud.gdapi.model.impl;

import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.id.IdFormatterUtils;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlTransient;

public class WrappedResource extends ResourceImpl implements Resource {

    Schema schema;
    Object obj;
    Map<String, Object> priorityFields = new LinkedHashMap<String, Object>();
    Map<String, Object> additionalFields;
    Map<String, Field> resourceFields;
    boolean createTsFields = true;
    String method;
    Set<String> priorityFieldNames = null;
    IdFormatter idFormatter;

    public WrappedResource(IdFormatter idFormatter, Schema schema, Object obj, Map<String, Object> additionalFields, String method) {
        this(idFormatter, schema, obj, additionalFields, null, method);
    }

    public WrappedResource(IdFormatter idFormatter, Schema schema, Object obj, Map<String, Object> additionalFields, Set<String> priorityFieldNames, String
     method) {
        super();
        this.schema = schema;
        this.resourceFields = schema.getResourceFields();
        this.obj = obj;
        this.idFormatter = idFormatter;
        this.additionalFields = additionalFields;
        this.priorityFieldNames = priorityFieldNames;
        this.method = method;
        init();
    }

    public WrappedResource(IdFormatter idFormatter, Schema schema, Object obj, String method) {
        this(idFormatter, schema, obj, new HashMap<String,Object>(), method);
    }

    protected void addField(String key, Object value) {
        if ( priorityFieldNames != null && priorityFieldNames.contains(key) ) {
            priorityFields.put(key, value);
        } else {
            fields.put(key, value);
        }
    }

    protected void init() {
        for ( Map.Entry<String,Field> entry : resourceFields.entrySet() ) {
            String name = entry.getKey();
            if ( name.equals(TypeUtils.ID_FIELD) ) {
                continue;
            }
            Field field = entry.getValue();
            if ( ! field.isIncludeInList() || (!StringUtils.equalsIgnoreCase(method, "post") && field.isReadOnCreateOnly())) {
                continue;
            }
            Object value = additionalFields.remove(name);
            if ( value == null ) {
                value = field.getValue(obj);
            }
            addField(name, IdFormatterUtils.formatReference(field, idFormatter, value));
            if ( createTsFields && field.getTypeEnum() == FieldType.DATE && value instanceof Date ) {
                addField(name + "TS", ((Date)value).getTime());
            }
        }

        for ( Map.Entry<String, Object> entry : additionalFields.entrySet() ) {
            Object value = entry.getValue();
            String key = entry.getKey();
            Field field = resourceFields.get(key);
            if ( (field != null && field.isIncludeInList()) || isResource(value) ) {
                addField(key, value);
            }
        }

        Object id = idFormatter.formatId(getType(), getIdValue());
        if ( id != null ) {
            setId(id.toString());
        }

        if ( priorityFields.size() > 0 ) {
            Map<String,Object> sorted = new LinkedHashMap<String, Object>(priorityFields);
            sorted.putAll(fields);
            fields = sorted;
        }
    }

    protected boolean isResource(Object obj) {
        if ( obj instanceof Resource ) {
            return true;
        }

        if ( obj instanceof List<?> ) {
            List<?> list = (List<?>)obj;
            if ( list.size() == 0 || isResource(list.get(0)) ) {
                return true;
            }
        }

        return false;
    }

    protected Object getIdValue() {
        Field idField = schema.getResourceFields().get(TypeUtils.ID_FIELD);
        return idField == null ? null : idField.getValue(obj);
    }

    @Override
    public String getType() {
        return schema.getId();
    }

    @Override
    public Map<String, Object> getFields() {
        return fields;
    }

    @XmlTransient
    public boolean isCreateTsFields() {
        return createTsFields;
    }

    public void setCreateTsFields(boolean createTsFields) {
        this.createTsFields = createTsFields;
    }

    public Set<String> getPriorityFieldNames() {
        return priorityFieldNames;
    }

    @XmlTransient
    public void setPriorityFieldNames(Set<String> priorityFieldNames) {
        this.priorityFieldNames = priorityFieldNames;
    }

}
