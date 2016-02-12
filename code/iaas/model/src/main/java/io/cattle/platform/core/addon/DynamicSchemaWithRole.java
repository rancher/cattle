package io.cattle.platform.core.addon;

import io.cattle.platform.core.model.DynamicSchema;

public interface DynamicSchemaWithRole extends DynamicSchema{

    /**
     * Setter for <code>cattle.dynamic_schema_role.role</code>.
     */
    public void setRole(java.lang.String value);

    /**
     * Getter for <code>cattle.dynamic_schema_role.role</code>.
     */
    @javax.persistence.Column(name = "role", nullable = false, length = 255)
    public java.lang.String getRole();

}
