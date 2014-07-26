package io.cattle.platform.metadata.serializer;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.metadata.service.MetadataService;
import io.cattle.platform.metadata.util.MetadataConstants;
import io.cattle.platform.object.serialization.ObjectTypeSerializerPostProcessor;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.Map;

import javax.inject.Inject;

public class MetadataInstanceSerializer implements ObjectTypeSerializerPostProcessor {

    MetadataService metadataService;
    IdFormatter idFormatter;

    @Override
    public String[] getTypes() {
        return new String[] { InstanceConstants.TYPE };
    }

    @Override
    public void process(Object obj, String type, Map<String, Object> data) {
        if ( ! ( obj instanceof Instance ) ) {
            return;
        }

        if ( DataAccessor.fromDataFieldOf(obj)
                .withKey(MetadataConstants.METADATA_ATTACH)
                .withDefault(false)
                .as(Boolean.class) ) {
            Map<String,Object> metadata = metadataService.getMetadataForInstance((Instance)obj, idFormatter);

            if ( metadata != null ) {
                DataAccessor.fromDataFieldOf(data)
                    .withKey(MetadataConstants.METADATA)
                    .set(metadata);
            }
        }
    }

    public MetadataService getMetadataService() {
        return metadataService;
    }

    @Inject
    public void setMetadataService(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    public IdFormatter getIdFormatter() {
        return idFormatter;
    }

    @Inject
    public void setIdFormatter(IdFormatter idFormatter) {
        this.idFormatter = idFormatter;
    }

}
