package io.cattle.platform.api.setting;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.model.impl.ResourceImpl;

public class SettingResource extends ResourceImpl {

    String name;
    String value;
    String source;

    public SettingResource(String name) {
        setType("setting");
        setId(name);
        setName(name);
        setValue(ArchaiusUtil.getStringValue(name));
        setSource(ArchaiusUtil.getSource(name));
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
