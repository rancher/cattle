package io.cattle.platform.docker.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.api.model.DockerBuild;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class DockerInstancePostCreate extends AbstractObjectProcessLogic implements ProcessPostListener {

    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[]{"instance.create"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!InstanceConstants.CONTAINER_LIKE.contains(instance.getKind())) {
            return null;
        }

        Image image = objectManager.loadResource(Image.class, instance.getImageId());
        if (image == null) {
            return null;
        }

        DockerBuild build = DataAccessor.field(instance, DockerInstanceConstants.FIELD_BUILD,
                jsonMapper, DockerBuild.class);

        if (build == null) {
            return null;
        }

        String imageUuid = DataAccessor.fieldString(instance, InstanceConstants.FIELD_IMAGE_UUID);
        if (imageUuid != null) {
            String tag = StringUtils.removeStart(imageUuid, "docker:");
            build.setTag(tag);
        }

        objectManager.setFields(image, DockerInstanceConstants.FIELD_BUILD, build);

        return null;
    }
}
