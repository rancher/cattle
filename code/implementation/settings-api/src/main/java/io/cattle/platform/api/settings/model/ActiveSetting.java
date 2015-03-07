package io.cattle.platform.api.settings.model;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Setting;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(update = true)
public class ActiveSetting {

    String id;
    String name;
    String value;
    Object activeValue;
    boolean isInDb;
    String source;
    Setting setting;

    public ActiveSetting() {
    }

    public ActiveSetting(String name, Object activeValue, String source) {
        this.name = name;
        this.activeValue = activeValue;
        this.source = source;
    }

    public void setSetting(Setting setting) {
        this.id = setting.getId().toString();
        this.name = setting.getName();
        this.value = setting.getValue();
        this.isInDb = true;
        this.setting = setting;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Field(update = true)
    public String getValue() {
        if (value == null && !isInDb && activeValue != null) {
            return ArchaiusUtil.getString(name).get();
        }
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isInDb() {
        return isInDb;
    }

    public void setInDb(boolean isInDb) {
        this.isInDb = isInDb;
    }

    public Object getActiveValue() {
        return activeValue;
    }

    public void setActiveValue(Object activeValue) {
        this.activeValue = activeValue;
    }

    public String getId() {
        return id == null ? name : id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Field(include = false)
    public Setting getSetting() {
        return setting;
    }

}