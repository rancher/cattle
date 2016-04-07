package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.DynamicSchemaRoleTable.*;
import static io.cattle.platform.core.model.tables.DynamicSchemaTable.*;

import io.cattle.platform.core.addon.DynamicSchemaWithRole;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.core.model.tables.records.DynamicSchemaRoleRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.apache.commons.lang3.StringUtils;
import org.jooq.InsertValuesStep2;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.exception.InvalidResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicSchemaDaoImpl extends AbstractJooqDao implements DynamicSchemaDao {

    private static final Logger log = LoggerFactory.getLogger(DynamicSchemaDaoImpl.class);
    private static final ManagedThreadLocal<Map<CacheKey,DynamicSchema>> CACHE = new ManagedThreadLocal<Map<CacheKey,DynamicSchema>>() {
        @Override
        protected Map<CacheKey, DynamicSchema> initialValue() {
            return new HashMap<>();
        }
    };

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
                cache(schema.getName(), accountId, role, schema);
                recordsToReturn.add(schema);
            }
        }
        return recordsToReturn;
    }

    private DynamicSchema get(String name, long accountId, String role) {
        return CACHE.get().get(new CacheKey(name, accountId, role));
    }

    private DynamicSchema cache(String name, long accountId, String role, DynamicSchema schema) {
        if (schema != null) {
            CACHE.get().put(new CacheKey(name, accountId, role), schema);
        }
        return schema;
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
        if (name == null) {
            return null;
        }
        if (!name.contains("machine") && !name.contains("Config")) {
            return null;
        }
        DynamicSchema schema = get(name, accountId, role);
        if (schema != null) {
            return schema;
        }

        return cache(name, accountId, role, getSchemaInternal(name, accountId, role));
    }

    private DynamicSchema getSchemaInternal(String name, long accountId, String role) {

        List<Record> records = schemaQuery(accountId, role)
                .and(DYNAMIC_SCHEMA.NAME.eq(name))
                .orderBy(DYNAMIC_SCHEMA.CREATED.asc())
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
            }
        }
        return record == null ? null : record.into(DynamicSchema.class);
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
        CACHE.get().clear();
    }

    @Override
    public void removeRoles(DynamicSchema dynamicSchema) {
        CACHE.get().clear();
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

    class CacheKey {
        String name;
        long accountId;
        String role;

        public CacheKey(String name, long accountId, String role) {
            super();
            this.name = name;
            this.accountId = accountId;
            this.role = role;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + (int) (accountId ^ (accountId >>> 32));
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((role == null) ? 0 : role.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CacheKey other = (CacheKey) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (accountId != other.accountId)
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (role == null) {
                if (other.role != null)
                    return false;
            } else if (!role.equals(other.role))
                return false;
            return true;
        }

        private DynamicSchemaDaoImpl getOuterType() {
            return DynamicSchemaDaoImpl.this;
        }
    }
}
