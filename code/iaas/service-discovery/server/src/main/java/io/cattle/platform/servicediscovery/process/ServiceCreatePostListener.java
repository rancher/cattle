package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.DynamicSchemaTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringListProperty;

public class ServiceCreatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener {

    private static final DynamicStringListProperty ALLOWED_TYPES = ArchaiusUtil.getList("service.create.schema.allowed.types");
    private static final Logger log = LoggerFactory.getLogger(ServiceCreatePostListener.class);

    @Inject
    DynamicSchemaDao dynamicSchemaDao;

    SchemaFactory schemaFactory;
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_CREATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service)state.getResource();
        Map<?, ?> serviceSchemas = DataAccessor.field(service, ServiceDiscoveryConstants.FIELD_SERVICE_SCHEMAS, Map.class);
        Schema baseSchema = schemaFactory.getSchema(Service.class);

        if (serviceSchemas == null) {
            return null;
        }

        if (baseSchema == null) {
            log.error("Failed to find schema for {}", Service.class);
            return null;
        }

        for (Map.Entry<?, ?> entry : serviceSchemas.entrySet()) {
            String name = entry.getKey().toString();

            if (schemaFactory.getSchema(name) != null) {
                continue;
            }

            SchemaImpl schema = jsonMapper.convertValue(entry.getValue(), SchemaImpl.class);
            schema.setId(name);

            try {
                saveService(service, merge(baseSchema, schema));
            } catch (IOException e) {
                log.error("Failed to save dynamic schema for [{}] [{}]", entry.getKey(), entry.getValue());
            }
        }

        return null;
    }

    protected SchemaImpl merge(Schema baseSchema, SchemaImpl newSchema) {
        Set<String> allowedTypes = new HashSet<>(ALLOWED_TYPES.get());

        SchemaImpl mergedSchema = new SchemaImpl();
        mergedSchema.setParent(baseSchema.getId());
        mergedSchema.setId(newSchema.getId());

        Map<String, Field> resourceFields = newSchema.getResourceFields();
        Map<String, Field> baseResourceFields = baseSchema.getResourceFields();

        for (String fieldName : resourceFields.keySet()) {
            Field newField = resourceFields.get(fieldName);
            if (newField == null || baseResourceFields.containsKey(fieldName)) {
                continue;
            }

            if (!allowedTypes.contains(newField.getType())) {
                continue;
            }

            mergedSchema.getResourceFields().put(fieldName, newField);
        }

        return mergedSchema;
    }

    protected void saveService(Service service, Schema schema) throws IOException {
        DynamicSchema dynamicSchema = dynamicSchemaDao.getSchema(schema.getId(), service.getAccountId(),
                service.getId());
        if (dynamicSchema != null) {
            return;
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        jsonMapper.writeValue(os, schema);

        objectManager.create(DynamicSchema.class,
                DYNAMIC_SCHEMA.NAME, schema.getId(),
                DYNAMIC_SCHEMA.ACCOUNT_ID, service.getAccountId(),
                DYNAMIC_SCHEMA.DEFINITION, new String(os.toByteArray(), "UTF-8"),
                DYNAMIC_SCHEMA.SERVICE_ID, service.getId(),
                DYNAMIC_SCHEMA.PARENT, schema.getParent());
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

}