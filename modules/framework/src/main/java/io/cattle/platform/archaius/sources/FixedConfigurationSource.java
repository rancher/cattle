package io.cattle.platform.archaius.sources;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class FixedConfigurationSource extends AbstractConfiguration implements NamedConfigurationSource {

    Configuration config;

    public FixedConfigurationSource(Configuration config) {
        this.config = config;
    }

    public AbstractConfiguration getAbstractConfig() {
        if (config instanceof AbstractConfiguration) {
            return (AbstractConfiguration) config;
        }
        return this;
    }

    @Override
    public Configuration subset(String prefix) {
        return config.subset(prefix);
    }

    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public boolean containsKey(String key) {
        return config.containsKey(key);
    }

    @Override
    public void addProperty(String key, Object value) {
        config.addProperty(key, value);
    }

    @Override
    protected void addPropertyDirect(String key, Object value) {
        throw new IllegalStateException();
    }

    @Override
    public void setProperty(String key, Object value) {
        config.setProperty(key, value);
    }

    @Override
    public void clearProperty(String key) {
        config.clearProperty(key);
    }

    @Override
    public void clear() {
        config.clear();
    }

    @Override
    public Object getProperty(String key) {
        return config.getProperty(key);
    }

    @Override
    public Iterator<String> getKeys(String prefix) {
        return config.getKeys(prefix);
    }

    @Override
    public Iterator<String> getKeys() {
        return config.getKeys();
    }

    @Override
    public Properties getProperties(String key) {
        return config.getProperties(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return config.getBoolean(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    @Override
    public byte getByte(String key) {
        return config.getByte(key);
    }

    @Override
    public byte getByte(String key, byte defaultValue) {
        return config.getByte(key, defaultValue);
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        return config.getByte(key, defaultValue);
    }

    @Override
    public double getDouble(String key) {
        return config.getDouble(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return config.getDouble(key, defaultValue);
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        return config.getDouble(key, defaultValue);
    }

    @Override
    public float getFloat(String key) {
        return config.getFloat(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return config.getFloat(key, defaultValue);
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        return config.getFloat(key, defaultValue);
    }

    @Override
    public int getInt(String key) {
        return config.getInt(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return config.getInt(key, defaultValue);
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        return config.getInteger(key, defaultValue);
    }

    @Override
    public long getLong(String key) {
        return config.getLong(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    @Override
    public short getShort(String key) {
        return config.getShort(key);
    }

    @Override
    public short getShort(String key, short defaultValue) {
        return config.getShort(key, defaultValue);
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        return config.getShort(key, defaultValue);
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        return config.getBigDecimal(key);
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return config.getBigDecimal(key, defaultValue);
    }

    @Override
    public BigInteger getBigInteger(String key) {
        return config.getBigInteger(key);
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        return config.getBigInteger(key, defaultValue);
    }

    @Override
    public String getString(String key) {
        return config.getString(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    @Override
    public String[] getStringArray(String key) {
        return config.getStringArray(key);
    }

    @Override
    public List<Object> getList(String key) {
        return config.getList(key);
    }

    @Override
    public List<Object> getList(String key, List<Object> defaultValue) {
        return config.getList(key, defaultValue);
    }

    @Override
    public String getSourceName() {
        return ArchaiusUtil.toSourceName(config);
    }
}
