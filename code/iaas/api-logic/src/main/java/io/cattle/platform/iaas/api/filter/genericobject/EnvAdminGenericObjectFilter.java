package io.cattle.platform.iaas.api.filter.genericobject;

import io.cattle.platform.core.constants.GenericObjectConstants;
import io.cattle.platform.core.model.tables.records.GenericObjectRecord;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnvAdminGenericObjectFilter extends AbstractDefaultResourceManagerFilter {

    @Override
    public String[] getTypes() {
        return new String[] {"genericObject"};
    }

    private Map<Object, Object> setfilterEnvAdminCondition(Map<Object, Object> criteria) {
        if (criteria == null) {
            criteria = new LinkedHashMap<Object, Object>();
        }
        Condition notEnvAdminGenericObjectCondition = new Condition(ConditionType.NE, GenericObjectConstants.ENV_ADMIN_TYPE);

        Object key = ObjectMetaDataManager.KIND_FIELD;
        Object value = Arrays.asList(notEnvAdminGenericObjectCondition);
        Object existing = criteria.get(key);

        if (existing instanceof List) {
            List<Object> newCondition = new ArrayList<Object>();
            newCondition.add(value);
            newCondition.addAll((List<?>)existing);
            criteria.put(key, newCondition);
        } else if (existing == null) {
            criteria.put(key, value);
        } else {
            criteria.put(key, Arrays.asList(value, existing));
        }
        return criteria;
    }

    private boolean isEnvAdminGenericObject(String type, String id, ApiRequest request, ResourceManager next) {
        Object obj = next.getById(type, id, new ListOptions(request));
        if(obj != null && obj instanceof GenericObjectRecord){
            if(GenericObjectConstants.ENV_ADMIN_TYPE.equals(((GenericObjectRecord)obj).getKind())){
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getById(String type, String id, ListOptions options, ResourceManager next) {
        Object obj = next.getById(type, id, options);
        if(obj != null && obj instanceof GenericObjectRecord){
            if(GenericObjectConstants.ENV_ADMIN_TYPE.equals(((GenericObjectRecord)obj).getKind())){
                return null;
            }
        }
        return obj;
    }

    @Override
    public Object getLink(String type, String id, String link, ApiRequest request, ResourceManager next) {
        if(isEnvAdminGenericObject(type, id, request, next)){
            return null;
        }
        return next.getLink(type, id, link, request);
    }

    @Override
    public Object list(String type, ApiRequest request, ResourceManager next) {
        Map<Object, Object> criteria = setfilterEnvAdminCondition(new LinkedHashMap<Object, Object>(request.getConditions()));
        return next.list(type, criteria, new ListOptions(request));
    }

    @Override
    public List<?> list(String type, Map<Object, Object> criteria, ListOptions options, ResourceManager next) {
        return next.list(type, setfilterEnvAdminCondition(criteria), options);
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<Object, Object> properties = CollectionUtils.toMap(request.getRequestObject());
        if(GenericObjectConstants.ENV_ADMIN_TYPE.equals(properties.get(ObjectMetaDataManager.KIND_FIELD))){
            return null;
        }
        return next.create(type, request);
    }

    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        if(isEnvAdminGenericObject(type, id, request, next)){
            return null;
        }
        return next.update(type, id, request);
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        if(isEnvAdminGenericObject(type, id, request, next)){
            return null;
        }
        return next.delete(type, id, request);
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if(isEnvAdminGenericObject(type, request.getId(), request, next)){
            return null;
        }
        return next.resourceAction(type, request);
    }

}
