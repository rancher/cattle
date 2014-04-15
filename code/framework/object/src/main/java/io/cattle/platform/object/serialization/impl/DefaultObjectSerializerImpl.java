package io.cattle.platform.object.serialization.impl;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.serialization.ObjectTypeSerializerPostProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultObjectSerializerImpl implements ObjectSerializer {

    JsonMapper jsonMapper;
    ObjectManager objectManager;
    ObjectMetaDataManager metaDataManager;
    Action action;
    String expression;
    Map<String,ObjectTypeSerializerPostProcessor> postProcessors;

    public DefaultObjectSerializerImpl(JsonMapper jsonMapper, ObjectManager objectManager,
            ObjectMetaDataManager metaDataManager, Map<String,ObjectTypeSerializerPostProcessor> postProcessors,
            Action action, String expression) {
        super();
        this.jsonMapper = jsonMapper;
        this.objectManager = objectManager;
        this.metaDataManager = metaDataManager;
        this.action = action;
        this.expression = expression;
        this.postProcessors = postProcessors;
    }

    @Override
    public Map<String, Object> serialize(Object obj) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(action.getName(), marshall(obj, action.getName(), action));
        return data;
    }

    protected Map<String,Object> marshall(Object obj, String type, Action action) {
        Map<String,Object> data = obj == null ? null : jsonMapper.writeValueAsMap(obj);

        if ( data != null ) {
            data.put("type", type);
        }

        for ( Action childAction : action.getChildren() ) {
            Relationship rel = metaDataManager.getRelationship(type, childAction.getName().toLowerCase());
            if ( rel == null ) {
                throw new IllegalStateException("Failed to find link for [" + childAction.getName() + "]");
            }

            Class<?> clz = rel.getObjectType();
            String childType = objectManager.getType(clz);

            if ( rel.isListResult() ) {
                List<Map<String,Object>> childData = new ArrayList<Map<String,Object>>();

                for ( Object childObject : objectManager.getListByRelationship(obj, rel) ) {
                    childData.add(marshall(childObject, childType, childAction));
                }

                if ( data != null ) {
                    data.put(rel.getName(), childData);
                }
            } else {
                Object childObject = objectManager.getObjectByRelationship(obj, rel);

                Map<String,Object> childData = marshall(childObject, childType, childAction);
                if ( data != null ) {
                    data.put(rel.getName(), childData);
                }
            }
        }

        ObjectTypeSerializerPostProcessor postProcessor = postProcessors.get(type);
        if ( postProcessor != null ) {
            postProcessor.process(obj, type, data);
        }

        return data;
    }

    @Override
    public String getExpression() {
        return expression;
    }
}
