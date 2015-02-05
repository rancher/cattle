package io.cattle.platform.api.utils;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.auth.impl.DefaultPolicy;
import io.cattle.platform.object.meta.ActionDefinition;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Action;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.WrappedResource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang3.StringUtils;

public class ApiUtils {

    public static final String SINGLE_ATTACHMENT_PREFIX = "s__";

    private static final Policy DEFAULT_POLICY = new DefaultPolicy();

    private static final ManagedThreadLocal<Integer> DEPTH = new ManagedThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    public static Object getFirstFromList(Object obj) {
        if ( obj instanceof Collection ) {
            return getFirstFromList(((Collection)obj).getData());
        }

        if ( obj instanceof List ) {
            List<?> list = (List<?>)obj;
            return list.size() > 0 ? list.get(0) : null;
        }

        return null;
    }

    public static Policy getPolicy() {
        Object policy = ApiContext.getContext().getPolicy();
        if ( policy instanceof Policy ) {
            return (Policy)policy;
        } else {
            return DEFAULT_POLICY;
        }
    }

    public static <T> List<T> authorize(List<T> list) {
        return getPolicy().authorizeList(list);
    }

    @SuppressWarnings("unchecked")
    public static <T> T authorize(T obj) {
        if ( obj instanceof List ) {
            return (T) authorize((List<T>)obj);
        }
        return getPolicy().authorizeObject(obj);
    }

    public static String getAttachementKey(Object obj) {
        return getAttachementKey(obj, ObjectUtils.getId(obj));
    }

    public static String getAttachementKey(Object obj, Object id) {
        if ( obj == null ) {
            return null;
        }

        if ( id == null ) {
            return null;
        }

        return obj.getClass().getName() + ":" + id;
    }

    public static void addAttachement(Object key, String name, Object obj) {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        @SuppressWarnings("unchecked")
        Map<String,Map<Object,Object>> attachments = (Map<String, Map<Object,Object>>) request.getAttribute(key);

        Object id = ObjectUtils.getId(obj);
        if ( id == null ) {
            return;
        }

        if ( attachments == null ) {
            attachments = new HashMap<String, Map<Object,Object>>();
            request.setAttribute(key, attachments);
        }

        Map<Object,Object> attachment = attachments.get(name);
        if ( attachment == null ) {
            attachment = new LinkedHashMap<Object, Object>();
            attachments.put(name, attachment);
        }

        attachment.put(id, obj);
    }

    public static Map<String,Object> getAttachements(Object obj, Transformer transformer) {
        Map<String,Object> result = new LinkedHashMap<String, Object>();
        Object key = getAttachementKey(obj);
        ApiRequest request = ApiContext.getContext().getApiRequest();
        @SuppressWarnings("unchecked")
        Map<String,Map<Object,Object>> attachments = (Map<String, Map<Object,Object>>) request.getAttribute(key);

        if ( attachments == null ) {
            return result;
        }

        for ( Map.Entry<String, Map<Object,Object>> entry : attachments.entrySet() ) {
            String keyName = entry.getKey();
            List<Object> objects = new ArrayList<Object>();
            for ( Object attachment : entry.getValue().values() ) {
                attachment = transformer.transform(attachment);
                if ( attachment != null ) {
                    objects.add(attachment);
                }
            }

            if ( keyName.startsWith(SINGLE_ATTACHMENT_PREFIX) ) {
                Object attachedObj = objects.size() > 0 ? objects.get(0) : null;
                result.put(keyName.substring(SINGLE_ATTACHMENT_PREFIX.length()), attachedObj);
            } else {
                result.put(keyName, objects);
            }
        }

        return result;
    }

    public static Resource createResourceWithAttachments(final ResourceManager resourceManager, final ApiRequest request,
            final IdFormatter idFormatter, final Schema schema, Object obj, Map<String,Object> inputAdditionalFields) {
        Integer depth = DEPTH.get();

        try {
            DEPTH.set(depth + 1);
            Map<String,Object> additionalFields = new LinkedHashMap<String, Object>();
            additionalFields.putAll(DataUtils.getFields(obj));

            if ( inputAdditionalFields != null && inputAdditionalFields.size() > 0 ) {
                additionalFields.putAll(inputAdditionalFields);
            }

            if ( depth == 0 ) {
                Map<String,Object> attachments = ApiUtils.getAttachements(obj, new Transformer() {
                    @Override
                    public Object transform(Object input) {
                        input = ApiUtils.authorize(input);
                        if ( input == null )
                            return null;


                        return resourceManager.convertResponse(input, request);
                    }
                });

                additionalFields.putAll(attachments);
            }

            return new WrappedResource(idFormatter, schema, obj, additionalFields);
        } finally {
            DEPTH.set(depth);
        }
    }

    public static String getSchemaIdForDisplay(SchemaFactory factory, Object obj) {
        Schema schema = factory.getSchema(obj.getClass());

        if ( schema == null ) {
            return null;
        }

        if ( schema.getChildren().size() > 0 ) {
            String kind = ObjectUtils.getKind(obj);
            Schema kindSchema = factory.getSchema(kind);
            if ( kindSchema != null && schema.getChildren().contains(kindSchema.getId()) ) {
                return kindSchema.getId();
            }
        }

        return schema.getId();
    }

    public static void addActions(Object state, Map<String,ActionDefinition> defs, Object obj, SchemaFactory schemaFactory, Schema schema, Resource resource) {
        Map<String,Action> actions = schema.getResourceActions();

        if ( actions == null || actions.size() == 0 ) {
            return;
        }

        UrlBuilder urlBuilder = ApiContext.getUrlBuilder();

        for ( Map.Entry<String,Action> entry : actions.entrySet() ) {
            String name = entry.getKey();
            Action action = entry.getValue();

            if ( ! isValidAction(obj, action) ) {
                continue;
            }

            ActionDefinition def = defs.get(name);
            if ( def == null || def.getValidStates().contains(state) ) {
                resource.getActions().put(name, urlBuilder.actionLink(resource, name));
            }
        }
    }

    public static boolean isValidAction(Object obj, Action action) {
        Map<String,Object> attributes = action.getAttributes();

        if ( attributes == null || attributes.size() == 0 ) {
            return true;
        }

        String capability = org.apache.commons.lang3.ObjectUtils.toString(attributes.get("capability"), null);
        String state = org.apache.commons.lang3.ObjectUtils.toString(attributes.get(ObjectMetaDataManager.STATE_FIELD), null);

        if ( ! StringUtils.isBlank(capability) &&
                ! DataAccessor.fieldStringList(obj, ObjectMetaDataManager.CAPABILITIES_FIELD).contains(capability) ) {
            return false;
        }

        if ( ! StringUtils.isBlank(state) &&
                ! state.equals(io.cattle.platform.object.util.ObjectUtils.getState(obj)) ) {
            return false;
        }

        return true;
    }
}
