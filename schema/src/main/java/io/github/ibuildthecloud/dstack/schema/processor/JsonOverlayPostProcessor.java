package io.github.ibuildthecloud.dstack.schema.processor;

import io.github.ibuildthecloud.gdapi.factory.impl.SchemaFactoryImpl;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.SchemaCollection;
import io.github.ibuildthecloud.model.impl.SchemaImpl;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.IOUtils;

public class JsonOverlayPostProcessor implements SchemaPostProcessor {

    URL schemaFile;
    JsonMapper jsonMapper;
    Map<String, SchemaImpl> schemas = new HashMap<String, SchemaImpl>(); 

    @Override
    public SchemaImpl postProcessRegister(SchemaImpl schema, SchemaFactoryImpl factory) {
        SchemaImpl override = schemas.get(schema.getId());
        if ( override != null && override.getPluralName() != null ) {
            schema.setPluralName(override.getPluralName());
        }

        return schema;
    }

    @Override
    public SchemaImpl postProcess(SchemaImpl schema, SchemaFactoryImpl factory) {
        SchemaImpl override = schemas.get(schema.getId());
        try {
            if ( override != null ) {

                Map<String,Field> originalFields = schema.getResourceFields();
                Map<String,Field> overrideFields = override.getResourceFields();

                PropertyUtils.copyProperties(schema, override);

                for ( String field : overrideFields.keySet() ) {
                    Field originalField = originalFields.get(field);
                    Field overrideField = overrideFields.get(field);

                    if ( originalField == null ) {
                        continue;
                    }

                    for ( PropertyDescriptor desc : PropertyUtils.getPropertyDescriptors(overrideField) ) {
                        String name = desc.getName();
                        Method readMethod = desc.getReadMethod();
                        Method writeMethod = desc.getWriteMethod();

                        if ( readMethod == null || writeMethod == null ) {
                            continue;
                        }

                        Object newValue = PropertyUtils.getProperty(overrideField, name);
                        if ( newValue != null ) {
                            PropertyUtils.setProperty(originalField, name, newValue);
                        }
                    }

                    overrideFields.put(field, originalField);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        return schema;
    }

    @PostConstruct
    public void init() throws IOException {
        InputStream is = null;
        try {
            is = schemaFile.openStream();
            byte[] bytes = IOUtils.toByteArray(is);
            SchemaCollection collection = jsonMapper.readValue(bytes, SchemaCollection.class);
            for ( Schema schema : collection.getData() ) {
                if ( schema instanceof SchemaImpl && schema.getId() != null ) {
                    schemas.put(schema.getId(), (SchemaImpl)schema);
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public URL getSchemaFile() {
        return schemaFile;
    }

    public void setSchemaFile(URL schemaFile) {
        this.schemaFile = schemaFile;
    }

}
