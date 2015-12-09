package io.cattle.platform.docker.process.serializer;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.serialization.ObjectTypeSerializerPostProcessor;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class DockerContainerSerializer implements ObjectTypeSerializerPostProcessor {

    JsonMapper jsonMapper;
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] { InstanceConstants.TYPE };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void process(Object obj, String type, Map<String, Object> data) {
        if (!(obj instanceof Instance))
            return;

        Instance instance = (Instance) obj;
        if (!InstanceConstants.CONTAINER_LIKE.contains(instance.getKind()))
            return;

        List volumesFromContainerIds = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_VOLUMES_FROM).as(jsonMapper, List.class);
        List<Instance> containers = null;
        if (volumesFromContainerIds != null && !volumesFromContainerIds.isEmpty()) {
            Condition condition = new Condition(ConditionType.IN, volumesFromContainerIds);
            containers = objectManager.find(Instance.class, INSTANCE.ID, condition);
        }
        if (containers == null)
            containers = new ArrayList<Instance>();
        data.put(DockerInstanceConstants.EVENT_FIELD_VOLUMES_FROM, containers);

        List<Volume>volumes = InstanceHelpers.extractVolumesFromMounts(instance, objectManager);
        data.put(DockerInstanceConstants.EVENT_FIELD_VOLUMES_FROM_DVM, volumes);
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
