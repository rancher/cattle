package io.cattle.platform.storage.service.lock;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.BlockingLockDefinition;

import com.netflix.config.DynamicLongProperty;

public class ExternalTemplateRegister extends AbstractLockDefinition implements BlockingLockDefinition {

    private static final DynamicLongProperty WAIT = ArchaiusUtil.getLong("external.template.register.lock.wait.millis");

    public ExternalTemplateRegister(String uuid) {
        super("TEMPLATE.REGISTER." + uuid);
    }

    @Override
    public long getWait() {
        return WAIT.get();
    }

}
