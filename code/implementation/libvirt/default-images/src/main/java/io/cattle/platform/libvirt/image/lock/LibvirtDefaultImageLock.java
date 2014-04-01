package io.cattle.platform.libvirt.image.lock;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.lock.definition.AbstractLockDefinition;
import io.cattle.platform.lock.definition.BlockingLockDefinition;

import com.netflix.config.DynamicLongProperty;

public class LibvirtDefaultImageLock extends AbstractLockDefinition implements BlockingLockDefinition {

    private static final DynamicLongProperty WAIT = ArchaiusUtil.getLong("libvirt.default.template.lock.wait");

    public LibvirtDefaultImageLock() {
        super("LIBVIRT.DEFAULT.LOCK");
    }

    @Override
    public long getWait() {
        return WAIT.get();
    }

}
