package io.cattle.platform.api.settings.manager;

import static io.cattle.platform.core.model.tables.SettingTable.*;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.api.settings.model.ActiveSetting;
import io.cattle.platform.archaius.sources.NamedConfigurationSource;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Setting;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.impl.ResourceImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.Predicate;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringListProperty;

public class SettingManager extends AbstractJooqResourceManager {

    private static final Logger log = LoggerFactory.getLogger(SettingManager.class);
    private static final DynamicStringListProperty PUBLIC_SETTINGS = ArchaiusUtil.getList("settings.public");

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Setting.class, ActiveSetting.class };
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        Setting setting = request.proxyRequestObject(Setting.class);

        Object existing = getByIdInternal(type, setting.getName(), new ListOptions());
        if ( existing instanceof ActiveSetting && ((ActiveSetting)existing).isInDb() ) {
            return updateInternal(type, setting.getName(), existing, request);
        } else {
            return super.createInternal(type, request);
        }
    }

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest request) {
        type = request.getSchemaFactory().getSchemaName(Setting.class);

        Object result = null;

        if (obj instanceof ActiveSetting) {
            Setting setting = ((ActiveSetting) obj).getSetting();
            if (setting == null) {
                String settingType = request.getSchemaFactory().getSchemaName(Setting.class);
                result = doCreate(settingType, Setting.class, CollectionUtils.asMap((Object) SETTING.NAME, ((ActiveSetting) obj).getName(), SETTING.VALUE,
                        request.proxyRequestObject(Setting.class).getValue()));
            } else {
                obj = setting;
            }
        }

        if (result == null) {
            result = super.updateInternal(type, id, obj, request);
        }

        return result == null ? null : getByIdInternal(type, id, new ListOptions());
    }

    @Override
    protected Object getByIdInternal(String type, String id, ListOptions options) {
        type = ApiContext.getSchemaFactory().getSchemaName(Setting.class);

        Setting setting = (Setting) super.getByIdInternal(type, id, options);
        Configuration config = lookupConfiguration();

        if (config == null) {
            return setting;
        }

        if (setting == null) {
            return getSettingByName(id, config, true);
        } else {
            ActiveSetting activeSetting = getSettingByName(setting.getName(), config, true);
            activeSetting.setSetting(setting);
            return activeSetting;
        }
    }

    @Override
    protected Object removeFromStore(String type, String id, Object obj, ApiRequest request) {
        if (obj instanceof ActiveSetting) {
            id = ((ActiveSetting) obj).getId();
        } else if (obj instanceof Setting) {
            Long idL = ((Setting) obj).getId();
            id = idL == null ? id : idL.toString();
        }

        if (id == null) {
            return null;
        }

        try {
            int result = create().delete(SETTING).where(SETTING.ID.eq(new Long(id))).execute();

            if (result != 1) {
                log.error("While deleting type [{}] and id [{}] got a result of [{}]", type, id, result);
                throw new ClientVisibleException(ResponseCodes.CONFLICT);
            }

            if (obj instanceof ActiveSetting) {
                return getByIdInternal(type, ((ActiveSetting) obj).getName(), new ListOptions());
            } else {
                return obj;
            }
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        type = schemaFactory.getSchemaName(Setting.class);

        Configuration config = lookupConfiguration();
        if (config == null) {
            return super.listInternal(schemaFactory, type, criteria, options);
        }

        Object list = super.listInternal(schemaFactory, type, criteria, options);
        if (criteria.containsKey(TypeUtils.ID_FIELD)) {
            return list;
        }

        // TODO, need to make filtering on name work
        String value = null;
        List<ActiveSetting> result = new ArrayList<ActiveSetting>();

        for (ActiveSetting setting : getSettings((List<Setting>) CollectionUtils.toList(list), config)) {
            if (value == null) {
                result.add(setting);
            } else if (value.equals(setting.getName())) {
                result.add(setting);
            }
        }

        return result;
    }

    @Override
    protected Object authorize(Object object) {
        Predicate predicate = new SettingsFilter(PUBLIC_SETTINGS.get(), ApiContext.getContext().getApiRequest());
        if (object instanceof List<?>) {
            List<Object> list = new ArrayList<>((List<?>) object);
            org.apache.commons.collections.CollectionUtils.filter(list, predicate);
            return super.authorize(list);
        } else if (predicate.evaluate(object)){
            return super.authorize(object);
        }

        return null;
    }

    protected List<ActiveSetting> getSettings(List<Setting> settings, Configuration config) {
        Map<String, ActiveSetting> result = new TreeMap<String, ActiveSetting>();

        for (Setting setting : settings) {
            ActiveSetting activeSetting = getSettingByName(setting.getName(), config, false);
            activeSetting.setSetting(setting);
            result.put(activeSetting.getName(), activeSetting);
        }

        Iterator<String> iter = config.getKeys();
        while (iter.hasNext()) {
            String key = iter.next();

            if (result.containsKey(key) || !key.matches("^[a-z].*")) {
                /*
                 * The regexp check is specifically to avoid showing environment
                 * variables. Those tend to have sensitive info in them
                 */
                continue;
            }

            ActiveSetting activeSetting = getSettingByName(key, config, false);
            if (activeSetting != null) {
                result.put(activeSetting.getName(), activeSetting);
            }
        }

        return new ArrayList<ActiveSetting>(result.values());
    }

    protected ActiveSetting getSettingByName(String name, Configuration config, boolean checkDb) {
        if (name == null) {
            return null;
        }

        Object value = config.getProperty(name);
        Configuration source = null;
        if (config instanceof ConcurrentCompositeConfiguration) {
            source = ((ConcurrentCompositeConfiguration) config).getSource(name);
        } else if (config instanceof CompositeConfiguration) {
            source = ((CompositeConfiguration) config).getSource(name);
        }

        if (value != null) {
            value = value.toString();
        }

        ActiveSetting activeSetting = new ActiveSetting(name, value, toString(source));

        if (checkDb) {
            Setting setting = create().selectFrom(SETTING).where(SETTING.NAME.eq(name)).fetchAny();
            if (setting != null) {
                activeSetting.setSetting(setting);
            }
        }

        return activeSetting;
    }

    protected String toString(Configuration config) {
        if (config instanceof NamedConfigurationSource) {
            return ((NamedConfigurationSource) config).getSourceName();
        }

        if (config instanceof DynamicConfiguration) {
            return ((DynamicConfiguration) config).getSource().getClass().getName();
        }

        return config == null ? null : config.getClass().getName();
    }

    protected Configuration lookupConfiguration() {
        Object obj = DynamicPropertyFactory.getBackingConfigurationSource();

        if (obj instanceof Configuration) {
            return (Configuration) obj;
        }

        return null;
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    protected Resource createResource(Object obj, IdFormatter idFormatter, ApiRequest apiRequest) {
        Resource r = super.createResource(obj, idFormatter, apiRequest);
        if (r instanceof ResourceImpl) {
            ((ResourceImpl) r).setType("setting");
            Object id = r.getFields().get("name");
            if (id != null) {
                ((ResourceImpl) r).setId(id.toString());
            }
            r.getLinks().remove(UrlBuilder.SELF);
            // Recreate the SELF url
            r.getLinks();
            ((ResourceImpl) r).setType("activeSetting");
            ((ResourceImpl) r).getFields().put("baseType", "setting");
        }
        return r;
    }

}
