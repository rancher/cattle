package io.cattle.platform.core.cleanup;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jooq.Field;
import org.jooq.Table;

public class CleanableTable {
	
	public static final List<String> timestampFieldNamePrecedence = Arrays.asList(
			"remove_time",
			"removed",
			"end_time",
			"applied_updated",
			"expires",
			"created");

	public final Table<?> table;
	public final Field<Long> idField;
	public final Field<Date> removeField;
	
	private CleanableTable(Table<?> table) {
		this.table = table;
		this.idField = getIdField(table);
		this.removeField = getRemoveField(table);
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
		for (String fieldName : timestampFieldNamePrecedence) {
			for (Field<?> field : table.fields()) {
				if (fieldName.equals(field.getName())) {
					return (Field<Date>) field;
				}
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return " " + table.toString();
	}
	
	public static CleanableTable from(Table<?> table) {
		return new CleanableTable(table);
	}
}
