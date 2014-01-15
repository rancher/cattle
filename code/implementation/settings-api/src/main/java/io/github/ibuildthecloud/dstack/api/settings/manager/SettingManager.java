package io.github.ibuildthecloud.dstack.api.settings.manager;

import io.github.ibuildthecloud.dstack.api.resource.jooq.AbstractJooqResourceManager;
import io.github.ibuildthecloud.dstack.api.settings.model.ActiveSetting;
import io.github.ibuildthecloud.dstack.core.model.Setting;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.DynamicPropertyFactory;

public class SettingManager extends AbstractJooqResourceManager {

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Setting.class, ActiveSetting.class };
    }

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest request) {
        type = getSchemaFactory().getSchemaName(Setting.class);

        if ( obj instanceof ActiveSetting ) {
            obj = ((ActiveSetting)obj).getSetting();
        }

        if ( obj == null ) {
            return null;
        }

        Object result = super.updateInternal(type, id, obj, request);

        return result == null ? null : getByIdInternal(type, id, new ListOptions());
    }

    @Override
    protected Object getByIdInternal(String type, String id, ListOptions options) {
        type = getSchemaFactory().getSchemaName(Setting.class);

        Setting setting = (Setting)super.getByIdInternal(type, id, options);
        Configuration config = lookupConfiguration();

        if ( config == null ) {
            return setting;
        }

        if ( setting == null ) {
            return getSettingByName(id, config);
        } else {
            ActiveSetting activeSetting = getSettingByName(setting.getName(), config);
            activeSetting.setSetting(setting);
            return activeSetting;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object listInternal(String type, Map<Object, Object> criteria, ListOptions options) {
        type = getSchemaFactory().getSchemaName(Setting.class);

        Configuration config = lookupConfiguration();
        if ( config == null ) {
            return super.listInternal(type, criteria, options);
        }

        Object result = super.listInternal(type, criteria, options);
        if ( criteria.containsKey(TypeUtils.ID_FIELD) ) {
            return result;
        }
        return getSettings((List<Setting>)CollectionUtils.toList(result), config);
    }

    protected List<ActiveSetting> getSettings(List<Setting> settings, Configuration config) {

        Map<String,ActiveSetting> result = new TreeMap<String, ActiveSetting>();

        for ( Setting setting : settings ) {
            ActiveSetting activeSetting = getSettingByName(setting.getName(), config);
            activeSetting.setSetting(setting);
            result.put(activeSetting.getName(), activeSetting);
        }

        Iterator<String> iter = config.getKeys();
        while ( iter.hasNext() ) {
            String key = iter.next();

            if ( result.containsKey(key) || ! key.matches("^[a-z].*") ) {
                /* The regexp check is specifically to avoid showing environment variables.
                 * Those tend to have sensitive info in them
                 */
                continue;
            }

            ActiveSetting activeSetting = getSettingByName(key, config);
            if ( activeSetting != null ) {
                result.put(activeSetting.getName(), activeSetting);
            }
        }

        return new ArrayList<ActiveSetting>(result.values());
    }

    protected ActiveSetting getSettingByName(String name, Configuration config) {
        if ( name == null ) {
            return null;
        }

        Object value = config.getProperty(name);
        Configuration source = null;
        if ( config instanceof ConcurrentCompositeConfiguration ) {
            source = ((ConcurrentCompositeConfiguration)config).getSource(name);
        } else if ( config instanceof CompositeConfiguration ) {
            source = ((CompositeConfiguration)config).getSource(name);
        }

        if ( value == null && source == null ) {
            return null;
        }

        return new ActiveSetting(name, value, toString(source));

    }

    protected String toString(Configuration config) {
        if ( config instanceof DynamicConfiguration ) {
            return ((DynamicConfiguration)config).getSource().getClass().getName();
        }
        return config == null ? null : config.getClass().getName();
    }

    protected Configuration lookupConfiguration() {
      Object obj = DynamicPropertyFactory.getBackingConfigurationSource();

      if ( obj instanceof Configuration ) {
          return (Configuration)obj;
      }

      return null;
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

}
