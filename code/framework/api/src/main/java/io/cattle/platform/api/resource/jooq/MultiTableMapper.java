package io.cattle.platform.api.resource.jooq;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.model.Pagination;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RecordHandler;
import org.jooq.Table;

public class MultiTableMapper extends AbstractSequentialList<Object> implements RecordHandler<Record> {

    ObjectMetaDataManager metaDataManager;
    List<Table<?>> tables = new ArrayList<Table<?>>();
    Map<String, TableMapping> mappings = new HashMap<String, MultiTableMapper.TableMapping>();
    Map<String, TableMapping> fieldsMapping = new HashMap<String, TableMapping>();
    List<Field<?>> fields = new ArrayList<Field<?>>();
    Map<Object, Object> result = new LinkedHashMap<Object, Object>();
    int resultSize;
    Pagination pagination;
    Integer limit;

    public MultiTableMapper(ObjectMetaDataManager metaDataManager, Pagination pagination) {
        this.metaDataManager = metaDataManager;
        this.pagination = pagination;

        limit = pagination == null ? null : pagination.getLimit();
    }

    public MultiTableMapper map(Table<?> table) {
        return map("", table);
    }

    public MultiTableMapper map(String name, Table<?> table) {
        boolean emptyPrefix = name.equals("");

        TableMapping mapping = new TableMapping();
        String prefix = emptyPrefix ? "" : "table" + mappings.size() + "_";

        mapping.originalTable = table;
        mapping.keyName = name;
        mapping.prefix = prefix;
        mapping.aliasedTable = table.as(mapping.prefix + table.getName());
        mapping.originalFields = table.fields();
        mapping.aliasedFields = new Field<?>[mapping.originalFields.length];

        Field<?>[] unaliasedFields = mapping.aliasedTable.fields();

        for (int i = 0; i < mapping.aliasedFields.length; i++) {
            Field<?> field = unaliasedFields[i];
            Field<?> alias = emptyPrefix ? field : field.as(prefix + field.getName());
            mapping.aliasedFields[i] = alias;

            fieldsMapping.put(alias.getName(), mapping);
            fields.add(alias);
        }

        tables.add(mapping.aliasedTable);
        mappings.put(mapping.keyName, mapping);

        return this;
    }

    public List<Table<?>> getTables() {
        return tables;
    }

    public List<Field<?>> getFields() {
        return fields;
    }

    @Override
    public void next(Record record) {
        resultSize++;

        if (limit != null && resultSize > limit) {
            return;
        }

        Map<String, Object> objects = new HashMap<String, Object>();

        for (Field<?> field : record.fields()) {
            TableMapping mapping = fieldsMapping.get(field.getName());
            if (mapping == null) {
                continue;
            }

            Object obj = objects.get(mapping.keyName);
            if (obj == null) {
                obj = newObject(mapping);
                objects.put(mapping.keyName, obj);
            }

            String fieldName = StringUtils.removeStart(field.getName(), mapping.prefix);
            String propertyName = metaDataManager.lookupPropertyNameFromFieldName(mapping.originalTable.getRecordType(), fieldName);
            if (propertyName != null) {
                setProperty(obj, propertyName, record, field);
            }
        }

        Object rootObject = objects.remove("");
        Object id = ObjectUtils.getId(rootObject);
        String key = ApiUtils.getAttachementKey(rootObject, id);
        if (key == null) {
            return;
        }

        result.put(id, rootObject);

        for (Map.Entry<String, Object> entry : objects.entrySet()) {
            ApiUtils.addAttachement(key, entry.getKey(), entry.getValue());
        }
    }

    protected void setProperty(Object obj, String prop, Record record, Field<?> field) {
        try {
            PropertyDescriptor desc = PropertyUtils.getPropertyDescriptor(obj, prop);
            if (desc != null && desc.getWriteMethod() != null) {
                Object value = record.getValue(field, desc.getWriteMethod().getParameterTypes()[0]);
                PropertyUtils.setProperty(obj, prop, value);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to set property [" + prop + "] on [" + obj + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to set property [" + prop + "] on [" + obj + "]", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Failed to set property [" + prop + "] on [" + obj + "]", e);
        }
    }

    protected Object newObject(TableMapping mapping) {
        Class<?> clz = mapping.originalTable.getRecordType();
        try {
            return clz.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("Failed to construct [" + clz + "]", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to construct [" + clz + "]", e);
        }
    }

    private static class TableMapping {
        String keyName;
        Table<?> originalTable;
        Table<?> aliasedTable;
        String prefix;
        Field<?>[] originalFields;
        Field<?>[] aliasedFields;
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        return new ForwardListIterator(index, result.values().iterator());
    }

    @Override
    public int size() {
        return result.size();
    }

    public int getResultSize() {
        return resultSize;
    }

}
