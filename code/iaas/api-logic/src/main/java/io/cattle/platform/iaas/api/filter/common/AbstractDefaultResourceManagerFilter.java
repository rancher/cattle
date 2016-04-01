package io.cattle.platform.iaas.api.filter.common;

import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractResourceManagerFilter;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.regex.Pattern;

public class AbstractDefaultResourceManagerFilter extends AbstractResourceManagerFilter implements Priority {

    private static final Pattern DNS_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9](?!.*--)[a-zA-Z0-9-]*$");

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    protected void validateDNSPatternForName(String name) {
        if (name != null)  {
            if(!DNS_NAME_PATTERN.matcher(name).matches()) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                        "name");
            } else if (name.endsWith("-")){
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                        "name");
            }
        }
    }

    protected void validateLinkName(String linkName){
        if(linkName != null && !linkName.isEmpty()){
            if(linkName.startsWith(".") || linkName.endsWith(".") || linkName.contains("..")) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                        "name");
            }
            
            //split around a "."
            String[] parts = linkName.split("\\.");
            if(parts.length > 1) {
                //check total length <= 253
                if (linkName.length() > 253) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MAX_LENGTH_EXCEEDED,
                            "name");
                }
            } 

            for (String linkPart : parts) {
                if(linkPart.startsWith("-") || linkPart.endsWith("-") || linkPart.contains("--")) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                            "name");
                }
                //check length
                if (linkPart.length() < 1) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MIN_LENGTH_EXCEEDED,
                            "name");
                }
                if (linkPart.length() > 63) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MAX_LENGTH_EXCEEDED,
                            "name");
                }

            }
        }
    }

}
