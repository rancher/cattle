package io.cattle.platform.core.cleanup;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jooq.Field;
import org.jooq.Table;

public class CleanableTable {

    public static final List<String> TIMESTAMP_FIELD_NAME_PRECEDENCE = Arrays.asList(
            "removed",
            "end_time",
            "applied_updated",
            "expires",
            "created");

    public final Table<?> table;
    public final Field<Long> idField;
    public final Field<Date> removeField;

    private Integer rowsDeleted = 0;
    private Integer rowsSkipped = 0;

    private CleanableTable(Table<?> table) {
        this(table, null);
    }

    private CleanableTable(Table<?> table, Field<Date> removedField) {
        this.table = table;
        this.idField = getIdField(table);
        this.removeField = removedField == null ? getRemoveField(table) : removedField;
    }

    @SuppressWarnings("unchecked")
    private Field<Long> getIdField(Table<?> table) {
        for (Field<?> field : table.fields()) {
            if (field.getName().equals("id")) {
                return (Field<Long>) field;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Field<Date> getRemoveField(Table<?> table) {
        for (String fieldName : TIMESTAMP_FIELD_NAME_PRECEDENCE) {
            for (Field<?> field : table.fields()) {
                if (fieldName.equals(field.getName())) {
                    return (Field<Date>) field;
                }
            }
        }
        return null;
    }

    public void clearRowCounts() {
        rowsDeleted = 0;
        rowsSkipped = 0;
    }

    public Integer getRowsDeleted() {
        return rowsDeleted;
    }

    public void addRowsDeleted(Integer rowsDeleted) {
        this.rowsDeleted += rowsDeleted;
    }

    public Integer getRowsSkipped() {
        return rowsSkipped;
    }

    public void addRowsSkipped(Integer rowsSkipped) {
        this.rowsSkipped += rowsSkipped;
    }

    @Override
    public String toString() {
        return " " + table.toString();
    }

    public static CleanableTable from(Table<?> table) {
        return new CleanableTable(table);
    }

    public static CleanableTable from(Table<?> table, Field<Date> removedField) {
        return new CleanableTable(table, removedField);
    }
}
