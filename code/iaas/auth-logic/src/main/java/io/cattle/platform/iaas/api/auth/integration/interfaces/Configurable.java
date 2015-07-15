package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.util.type.Named;

public interface Configurable extends Named {

    boolean isConfigured();
}
