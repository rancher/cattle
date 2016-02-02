package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.DynamicSchemaRoleTable.*;
import static io.cattle.platform.core.model.tables.DynamicSchemaTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.addon.DynamicSchemaWithRole;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.records.DynamicSchemaRoleRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.jooq.InsertValuesStep2;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.exception.InvalidResultException;

public class DynamicSchemaDaoImpl extends AbstractJooqDao implements DynamicSchemaDao {

    private static final Logger log = LoggerFactory.getLogger(DynamicSchemaDaoImpl.class);

    @Override
    public List<? extends DynamicSchema> getSchemas(long accountId, String role) {
        List<Record> records = schemaQuery(accountId, role)
                .orderBy(DYNAMIC_SCHEMA.CREATED.asc())
                .fetch();
        Map<String, List<Record>> nameSchemas = new HashMap<>();
        for (Record record: records) {
            if (nameSchemas.get(record.getValue(DYNAMIC_SCHEMA.NAME)) == null) {
                nameSchemas.put(record.getValue(DYNAMIC_SCHEMA.NAME), new ArrayList<Record>());
            }
            nameSchemas.get(record.getValue(DYNAMIC_SCHEMA.NAME)).add(record);
        }
        List<DynamicSchema> recordsToReturn = new ArrayList<>();
        for (List<Record> list: nameSchemas.values()) {
            DynamicSchema schema = null;
            try {
                schema = pickRecordOnPriority(list, accountId, role);
            } catch (InvalidResultException e){
                log.error("Failed to get a schema record.", e);
            }
            if (schema != null) {
                recordsToReturn.add(schema);
            }
        }
        return recordsToReturn;
    }

    private SelectConditionStep<Record> schemaQuery(long accountId, String role) {
        return create()
                .select()
                .from(DYNAMIC_SCHEMA).leftOuterJoin(DYNAMIC_SCHEMA_ROLE)
                .on(DYNAMIC_SCHEMA_ROLE.DYNAMIC_SCHEMA_ID.eq(DYNAMIC_SCHEMA.ID))
                .where(DYNAMIC_SCHEMA.ACCOUNT_ID.eq(accountId)
                        .and(DYNAMIC_SCHEMA_ROLE.ROLE.eq(role))
                        .and(DYNAMIC_SCHEMA.STATE.ne(CommonStatesConstants.PURGED)))
                .or(DYNAMIC_SCHEMA.ACCOUNT_ID.eq(accountId)
                        .and(DYNAMIC_SCHEMA_ROLE.ROLE.isNull())
                        .and(DYNAMIC_SCHEMA.STATE.ne(CommonStatesConstants.PURGED)))
                .or(DYNAMIC_SCHEMA_ROLE.ROLE.eq(role)
                        .and(DYNAMIC_SCHEMA.ACCOUNT_ID.isNull())
                        .and(DYNAMIC_SCHEMA.STATE.ne(CommonStatesConstants.PURGED)));
    }

    @Override
    public DynamicSchema getSchema(String name, long accountId, String role) {

        List<Record> records = schemaQuery(accountId, role)
                .and(DYNAMIC_SCHEMA.NAME.eq(name))
                .fetch();

        if (records.size() == 0 && name != null && name.endsWith("s")) {
            return getSchema(TypeUtils.guessSingularName(name), accountId, role);
        }

        if (records.size() == 1) {
            return records.get(0).into(DynamicSchema.class);
        } else if (records.size() == 0) {
            return null;
        } else {
            return pickRecordOnPriority(records, accountId, role);
        }
    }

    private DynamicSchema pickRecordOnPriority(List<Record> records, long accountId, String role) {
        if (records.size() == 1) {
            return records.get(0).into(DynamicSchema.class);
        }
        Record record = null;
        int lastPriority = 0;
        for (Record r: records) {
            DynamicSchemaWithRole withRole = r.into(DynamicSchemaWithRole.class);
            int priority = 0;
            if (withRole.getAccountId() != null &&
                    withRole.getAccountId().equals(accountId) &&
                    StringUtils.equals(withRole.getRole(), role)) {
                priority = 3;
            } else if (withRole.getAccountId() != null &&
                    withRole.getAccountId().equals(accountId) &&
                    withRole.getRole() == null) {
                priority = 2;
            } else if (withRole.getAccountId() == null && role != null && withRole.getRole().equals(role)) {
                priority = 1;
            }
            if (priority > lastPriority) {
                lastPriority = priority;
                record = r;
            } else if (priority == lastPriority && priority != 0 &&
                    !record.getValue(DYNAMIC_SCHEMA.UUID).equals(withRole.getUuid())) {
                throw new InvalidResultException("Multiple dynamic schemas found of the" +
                        " same role and account id combination: Name:" + withRole.getName() +
                        "Role: " + withRole.getRole() + " AccountId: " + withRole.getAccountId());
            }
        }
        return record == null ? null : record.into(DynamicSchema.class);
    }

    @Override
    public DynamicSchema getSchema(String name, long accountId, long serviceId) {
        return create().selectFrom(DYNAMIC_SCHEMA)
                .where(DYNAMIC_SCHEMA.ACCOUNT_ID.eq(accountId)
                        .and(DYNAMIC_SCHEMA.SERVICE_ID.eq(serviceId))
                        .and(DYNAMIC_SCHEMA.NAME.eq(name)))
                .orderBy(DYNAMIC_SCHEMA.CREATED.asc())
                .fetchAny();
    }

    @Override
    public int deleteSchemas(long serviceId) {
        return create().delete(DYNAMIC_SCHEMA)
                .where(DYNAMIC_SCHEMA.SERVICE_ID.eq(serviceId))
                .execute();
    }

    @Override
    public List<Long> getAgentForService(Service service) {
        return Arrays.asList(create().select(INSTANCE.AGENT_ID)
                .from(DYNAMIC_SCHEMA)
                .join(SERVICE)
                    .on(SERVICE.ID.eq(DYNAMIC_SCHEMA.SERVICE_ID))
                .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .join(INSTANCE)
                    .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(DYNAMIC_SCHEMA.NAME.eq(service.getKind())
                        .and(DYNAMIC_SCHEMA.ACCOUNT_ID.eq(service.getAccountId()))
                        .and(INSTANCE.AGENT_ID.isNotNull())
                        .and(SERVICE.REMOVED.isNull())
                        .and(INSTANCE.REMOVED.isNull())
                        .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                ).fetch().intoArray(INSTANCE.AGENT_ID));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void createRoles(DynamicSchema dynamicSchema) {
        List<String> roles = (List<String>) CollectionUtils.toList(CollectionUtils.getNestedValue(dynamicSchema.getData(),
                "fields", "roles"));
        InsertValuesStep2<DynamicSchemaRoleRecord, Long, String> insertStart = create()
                .insertInto(DYNAMIC_SCHEMA_ROLE, DYNAMIC_SCHEMA_ROLE.DYNAMIC_SCHEMA_ID, DYNAMIC_SCHEMA_ROLE.ROLE);
        for (String role: roles) {
                    insertStart = insertStart.values(dynamicSchema.getId(), role);
        }
        insertStart.execute();
    }

    @Override
    public void removeRoles(DynamicSchema dynamicSchema) {
        create().delete(DYNAMIC_SCHEMA_ROLE)
                .where(DYNAMIC_SCHEMA_ROLE.DYNAMIC_SCHEMA_ID.eq(dynamicSchema.getId()))
                .execute();

    }

    @Override
    public boolean isUnique(String name, List<String> roles, Long accountId) {
        List<DynamicSchemaWithRole> schemas;
        if (accountId == null) {
            schemas = create()
                    .select()
                    .from(DYNAMIC_SCHEMA).leftOuterJoin(DYNAMIC_SCHEMA_ROLE)
                    .on(DYNAMIC_SCHEMA_ROLE.DYNAMIC_SCHEMA_ID.eq(DYNAMIC_SCHEMA.ID))
                    .where(DYNAMIC_SCHEMA_ROLE.ROLE.in(roles))
                    .and(DYNAMIC_SCHEMA.NAME.eq(name))
                    .and(DYNAMIC_SCHEMA.STATE.ne(CommonStatesConstants.PURGED))
                    .and(DYNAMIC_SCHEMA.ACCOUNT_ID.isNull())
                    .fetch().into(DynamicSchemaWithRole.class);
        } else if (roles == null || roles.isEmpty()) {
            return null == create().select().from(DYNAMIC_SCHEMA)
                    .leftOuterJoin(DYNAMIC_SCHEMA_ROLE)
                    .on(DYNAMIC_SCHEMA_ROLE.DYNAMIC_SCHEMA_ID.eq(DYNAMIC_SCHEMA.ID))
                    .where(DYNAMIC_SCHEMA.ACCOUNT_ID.eq(accountId))
                    .and(DYNAMIC_SCHEMA.NAME.eq(name))
                    .and(DYNAMIC_SCHEMA_ROLE.ROLE.isNull())
                    .fetchAny();
        } else {
            schemas = create()
                    .select()
                    .from(DYNAMIC_SCHEMA).leftOuterJoin(DYNAMIC_SCHEMA_ROLE)
                    .on(DYNAMIC_SCHEMA_ROLE.DYNAMIC_SCHEMA_ID.eq(DYNAMIC_SCHEMA.ID))
                    .where(DYNAMIC_SCHEMA_ROLE.ROLE.in(roles))
                    .and(DYNAMIC_SCHEMA.NAME.eq(name))
                    .and(DYNAMIC_SCHEMA.ACCOUNT_ID.eq(accountId))
                    .and(DYNAMIC_SCHEMA.STATE.ne(CommonStatesConstants.PURGED))
                    .fetch().into(DynamicSchemaWithRole.class);
        }
        if (schemas.isEmpty()) {
            return true;
        }

        for (DynamicSchemaWithRole schema: schemas) {
            if (roles.contains(schema.getRole())) {
                return false;
            }
        }
        return true;

    }

}
