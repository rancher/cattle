package io.cattle.platform.db.jooq.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Table;

public abstract class MultiRecordMapper<T> implements RecordMapper<Record, T> {

    protected List<Table<?>> tables = new ArrayList<Table<?>>();
    protected List<Class<? extends Record>> classes = new ArrayList<Class<? extends Record>>();
    protected Map<String,Target> targets = new HashMap<String, MultiRecordMapper.Target>();
    protected List<Field<?>> fields = new ArrayList<Field<?>>();
    protected int count = 0;

    @SuppressWarnings({ "unchecked", "hiding" })
    public <T extends Table<?>> T add(T input) {
        int index = count++;
        String prefix = String.format("%s_%d", input.getName(), index);
        Table<?> alias = input.as(prefix);

        for ( Field<?> field : alias.fields() ) {
            String fieldAlias = String.format("%s_%s", prefix, field.getName());
            Target target = new Target(field.getName(), index);

            targets.put(fieldAlias, target);
            fields.add(field.as(String.format("%s_%s", prefix, field.getName())));
        }

        classes.add(input.getRecordType());

        return (T)alias;
    }

    @Override
    public T map(Record record) {
        List<Map<String,Object>> maps = new ArrayList<Map<String,Object>>(classes.size());
        for ( int i = 0 ; i < classes.size() ; i++ ) {
            maps.add(new HashMap<String,Object>());
        }

        Map<String,Object> row = record.intoMap();

        for ( Map.Entry<String, Object> entry : row.entrySet() ) {
            Target target = targets.get(entry.getKey());
            if ( target == null ) {
                continue;
            }

            maps.get(target.index).put(target.fieldName, entry.getValue());
        }

        List<Object> result = new ArrayList<Object>();

        for ( int i = 0 ; i < maps.size() ; i++ ) {
            try {
                Record resultRecord = classes.get(i).newInstance();
                resultRecord.fromMap(maps.get(i));
                resultRecord.changed(false);
                result.add(resultRecord);
            } catch (InstantiationException e) {
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        return map(result);
    }

    protected abstract T map(List<Object> input);

    public List<Field<?>> fields() {
        return fields;
    }

    private final static class Target {
        String fieldName;
        int index;

        public Target(String fieldName, int index) {
            super();
            this.fieldName = fieldName;
            this.index = index;
        }
    }

}