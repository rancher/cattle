package io.cattle.platform.iaas.api.filter.common;

import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractResourceManagerFilter;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.regex.Pattern;

public class AbstractDefaultResourceManagerFilter extends AbstractResourceManagerFilter implements Priority {

    private static final Pattern DNS_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9](?!.*--)[a-zA-Z0-9-]*[a-zA-Z0-9]$");

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    protected void validateDNSPatternForName(String name) {
        if (name != null && !DNS_NAME_PATTERN.matcher(name).matches()) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                    "name");
        }
    }

}
