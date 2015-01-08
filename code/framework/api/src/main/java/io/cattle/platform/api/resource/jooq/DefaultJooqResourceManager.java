package io.cattle.platform.api.resource.jooq;

public class DefaultJooqResourceManager extends AbstractJooqResourceManager {

    // private static final Logger log =
    // LoggerFactory.getLogger(DefaultJooqResourceManager.class);

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

}
