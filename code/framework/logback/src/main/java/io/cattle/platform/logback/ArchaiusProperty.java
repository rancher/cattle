package io.cattle.platform.logback;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.PropertyDefinerBase;
import ch.qos.logback.core.spi.PropertyDefiner;

public class ArchaiusProperty extends PropertyDefinerBase implements PropertyDefiner {

    private static final Logger log = LoggerFactory.getLogger(ArchaiusProperty.class);

    String name;

    @Override
    public String getPropertyValue() {
        String value = ArchaiusUtil.getString(name).get();
        log.info("Logback [{}] = [{}]", name, value);
        return value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
