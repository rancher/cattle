package io.cattle.platform.api.formatter;

import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.id.TypeIdFormatter;

public class DefaultIdFormatter extends TypeIdFormatter implements Priority {

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    @Override
    protected TypeIdFormatter newFormatter() {
        return new DefaultIdFormatter();
    }

}
