package io.cattle.platform.iaas.api.auth.integration.local;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(create = true, list = false)
public interface ChangePassword {

    @Field(create = true)
    String getOldSecret();

    @Field(create = true)
    String getNewSecret();
}
