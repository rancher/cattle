package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.DynamicSchema;

import java.util.List;

public interface DynamicSchemaDao {

    List<? extends DynamicSchema> getSchemas(long accountId, String role);

    DynamicSchema getSchema(String name, long accountId, String role);

    void createRoles(DynamicSchema dynamicSchema);

    void removeRoles(DynamicSchema dynamicSchema);

    boolean isUnique(String name, List<String> roles, Long accountId);

    class CacheKey {
        public String type;
        public long accountId;
        public String role;

        public CacheKey(String type, long accountId, String role) {
            super();
            this.type = type;
            this.accountId = accountId;
            this.role = role;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (accountId ^ (accountId >>> 32));
            result = prime * result + ((role == null) ? 0 : role.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
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
            if (accountId != other.accountId)
                return false;
            if (role == null) {
                if (other.role != null)
                    return false;
            } else if (!role.equals(other.role))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }
    }
}
