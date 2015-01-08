package io.cattle.platform.schema.processor;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.AbstractSchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.impl.FieldImpl;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.apache.commons.beanutils.PropertyUtils;

public class JpaWritablePostProcessor extends AbstractSchemaPostProcessor implements SchemaPostProcessor {

    @Override
    public SchemaImpl postProcess(SchemaImpl schema, SchemaFactory factory) {
        Class<?> clz = factory.getSchemaClass(schema.getId());
        if (clz == null || clz.getAnnotation(Entity.class) == null) {
            return schema;
        }

        schema.setCreate(true);
        schema.setUpdate(true);
        schema.setDeletable(true);

        for (PropertyDescriptor prop : PropertyUtils.getPropertyDescriptors(clz)) {
            processProperty(schema, prop);
        }

        return schema;
    }

    protected void processProperty(SchemaImpl schema, PropertyDescriptor prop) {
        if (TypeUtils.ID_FIELD.equals(prop.getName())) {
            return;
        }

        FieldImpl field = getField(schema, prop.getName());
        Method writeMethod = prop.getWriteMethod();
        Method readMethod = prop.getReadMethod();

        if (field == null || writeMethod == null) {
            return;
        }

        Column column = writeMethod.getAnnotation(Column.class);
        if (column == null && readMethod != null) {
            column = readMethod.getAnnotation(Column.class);
        }

        if (column == null) {
            return;
        }

        field.setNullable(field.getTypeEnum() == FieldType.STRING ? true : column.nullable());
        if (column.length() > 0) {
            field.setMaxLength((long) column.length());
        }

        if (!prop.getName().equals(TypeUtils.ID_FIELD)) {
            field.setCreate(true);
        }

        field.setUpdate(true);
    }

    protected FieldImpl getField(SchemaImpl schema, String name) {
        Field field = schema.getResourceFields().get(name);
        if (field instanceof FieldImpl) {
            return (FieldImpl) field;
        }

        return null;
    }

}
