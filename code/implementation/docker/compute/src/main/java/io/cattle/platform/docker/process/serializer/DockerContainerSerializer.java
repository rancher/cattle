package io.cattle.platform.docker.process.serializer;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.serialization.ObjectTypeSerializerPostProcessor;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.HashMap;
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
        if (!instance.getKind().equals(InstanceConstants.TYPE_CONTAINER))
            return;

        List volumesFromContainerIds = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_VOLUMES_FROM).as(jsonMapper, List.class);
        List<Instance> containers = null;
        if (volumesFromContainerIds != null && !volumesFromContainerIds.isEmpty()) {
            Map ids = new HashMap();
            ids.put(INSTANCE.ID, volumesFromContainerIds);
            containers = objectManager.find(Instance.class, ids);
        }
        if (containers == null)
            containers = new ArrayList<Instance>();
        data.put(DockerInstanceConstants.EVENT_FIELD_VOLUMES_FROM, containers);
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
