package io.cattle.platform.docker.process.dao.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.docker.process.dao.ComposeDao;
import io.cattle.platform.docker.process.util.DockerConstants;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.List;

import javax.inject.Inject;

public class ComposeDaoImpl extends AbstractJooqDao implements ComposeDao {

    @Inject
    ObjectManager objectManager;

    @Override
    public Environment getComposeProjectByName(long accountId, String name) {
        return objectManager.findAny(Environment.class,
                ObjectMetaDataManager.NAME_FIELD, name,
                ObjectMetaDataManager.ACCOUNT_FIELD, accountId,
                ObjectMetaDataManager.REMOVED_FIELD, null,
                ObjectMetaDataManager.STATE_FIELD, new Condition(ConditionType.NE, CommonStatesConstants.REMOVING),
                ObjectMetaDataManager.KIND_FIELD, DockerConstants.TYPE_COMPOSE_PROJECT);
    }

    @Override
    public Service getComposeServiceByName(long accountId, String name, String projectName) {
        List<? extends Service> services = create().select(SERVICE.fields())
            .from(SERVICE)
            .join(ENVIRONMENT)
                .on(ENVIRONMENT.ID.eq(SERVICE.ENVIRONMENT_ID))
            .where(SERVICE.ACCOUNT_ID.eq(accountId)
                    .and(SERVICE.NAME.eq(name))
                    .and(SERVICE.REMOVED.isNull())
                    .and(SERVICE.STATE.ne(CommonStatesConstants.REMOVING))
                    .and(SERVICE.KIND.eq(DockerConstants.TYPE_COMPOSE_SERVICE))
                    .and(ENVIRONMENT.NAME.eq(projectName))
                    .and(ENVIRONMENT.KIND.eq(DockerConstants.TYPE_COMPOSE_PROJECT))
                    .and(ENVIRONMENT.REMOVED.isNull())).fetchInto(ServiceRecord.class);

        return services.size() > 0 ? services.get(0) : null;
    }

}
