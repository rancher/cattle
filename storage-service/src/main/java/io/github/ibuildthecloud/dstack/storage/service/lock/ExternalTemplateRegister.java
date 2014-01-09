package io.github.ibuildthecloud.dstack.storage.service.lock;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;
import io.github.ibuildthecloud.dstack.lock.definition.BlockingLockDefinition;

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
