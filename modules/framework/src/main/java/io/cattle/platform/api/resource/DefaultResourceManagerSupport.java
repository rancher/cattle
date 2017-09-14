package io.cattle.platform.api.resource;

import io.cattle.platform.api.resource.jooq.JooqAccountAuthorization;
import io.cattle.platform.api.resource.jooq.JooqResourceListSupport;

public class DefaultResourceManagerSupport {

    ObjectResourceManagerSupport objectResourceManagerSupport;
    JooqResourceListSupport jooqResourceListSupport;
    JooqAccountAuthorization jooqAccountAuthorization;

    public DefaultResourceManagerSupport(ObjectResourceManagerSupport objectResourceManagerSupport, JooqResourceListSupport jooqResourceListSupport,
            JooqAccountAuthorization jooqAccountAuthorization) {
        super();
        this.objectResourceManagerSupport = objectResourceManagerSupport;
        this.jooqResourceListSupport = jooqResourceListSupport;
        this.jooqAccountAuthorization = jooqAccountAuthorization;
    }

    public ObjectResourceManagerSupport getObjectResourceManagerSupport() {
        return objectResourceManagerSupport;
    }

    public JooqResourceListSupport getJooqResourceListSupport() {
        return jooqResourceListSupport;
    }

    public JooqAccountAuthorization getJooqAccountAuthorization() {
        return jooqAccountAuthorization;
    }

}
