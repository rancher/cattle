package io.github.ibuildthecloud.dstack.object.serialization.impl;

import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.dstack.object.serialization.ObjectSerializer;

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

    public DefaultObjectSerializerImpl(JsonMapper jsonMapper, ObjectManager objectManager,
            ObjectMetaDataManager metaDataManager, Action action, String expression) {
        super();
        this.jsonMapper = jsonMapper;
        this.objectManager = objectManager;
        this.metaDataManager = metaDataManager;
        this.action = action;
        this.expression = expression;
    }

    @Override
    public Map<String, Object> serialize(Object obj) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(action.getName(), marshall(obj, action.getName(), action));
        return data;
    }

    protected Map<String,Object> marshall(Object obj, String type, Action action) {
        Map<String,Object> data = obj == null ? null : jsonMapper.writeValueAsMap(obj);

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

        return data;
    }

    @Override
    public String getExpression() {
        return expression;
    }
}
