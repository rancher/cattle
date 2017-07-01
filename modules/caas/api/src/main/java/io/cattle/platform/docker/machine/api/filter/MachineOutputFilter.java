package io.cattle.platform.docker.machine.api.filter;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.framework.secret.SecretsService;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MachineOutputFilter implements ResourceOutputFilter {

    private static final Logger log = LoggerFactory.getLogger(MachineOutputFilter.class);

    SecretsService serviceService;

    public MachineOutputFilter(SecretsService serviceService) {
        super();
        this.serviceService = serviceService;
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (!(original instanceof Host)) {
            return converted;
        }

        Object extracted = converted.getFields().get(HostConstants.EXTRACTED_CONFIG_FIELD);
        if (extracted instanceof String) {
            try {
                if (((String) extracted).startsWith("{")) {
                    extracted = serviceService.decrypt(((Host)original).getAccountId(), (String)extracted);
                }
            } catch (Exception e) {
                log.error("Failed to decrypt machine extracted config", e);
            }
            converted.getFields().put(HostConstants.EXTRACTED_CONFIG_FIELD, extracted);
        }

        return converted;
    }

}
