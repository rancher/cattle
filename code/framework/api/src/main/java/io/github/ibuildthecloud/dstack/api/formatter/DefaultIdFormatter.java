package io.github.ibuildthecloud.dstack.api.formatter;

import io.github.ibuildthecloud.dstack.util.type.Priority;
import io.github.ibuildthecloud.gdapi.id.TypeIdFormatter;

public class DefaultIdFormatter extends TypeIdFormatter implements Priority {

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
