package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.util.type.Named;

import java.util.Set;

public interface Scoped extends Named{

    Set<String> scopes();
}
