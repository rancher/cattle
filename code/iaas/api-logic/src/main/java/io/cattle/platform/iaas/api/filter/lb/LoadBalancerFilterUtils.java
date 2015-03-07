package io.cattle.platform.iaas.api.filter.lb;

import io.cattle.platform.core.dao.GenericMapDao;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.AbstractMap.SimpleEntry;

public class LoadBalancerFilterUtils {
    /**
     * @param mapType
     * @param leftResourceType
     * @param leftResourceId
     * @param rightResourceType
     * @param rightResourceId
     * @param apiField
     *            - single mapping of API field-to-validate (String) and map
     *            action performed (add/remove)
     */
    public void validateGenericMapAction(GenericMapDao mapDao, Class<?> mapType, Class<?> leftResourceType, long leftResourceId, Class<?> rightResourceType,
            long rightResourceId, SimpleEntry<String, Boolean> apiField) {

        if (apiField.getValue().booleanValue()) {
            if (mapDao.findNonRemoved(mapType, leftResourceType, leftResourceId, rightResourceType, rightResourceId) != null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE, apiField.getKey());
            }
        } else {
            if (mapDao.findToRemove(mapType, leftResourceType, leftResourceId, rightResourceType, rightResourceId) == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION, apiField.getKey());
            }
        }
    }
}
