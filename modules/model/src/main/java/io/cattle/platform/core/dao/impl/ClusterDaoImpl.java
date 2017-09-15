package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.core.model.tables.records.AccountRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.jooq.Configuration;

import java.util.Map;

import static io.cattle.platform.core.model.Tables.*;

public class ClusterDaoImpl extends AbstractJooqDao implements ClusterDao {

    TransactionDelegate transaction;
    ObjectManager objectManager;
    GenericResourceDao resourceDao;

    public ClusterDaoImpl(Configuration configuration, TransactionDelegate transaction, ObjectManager objectManager, GenericResourceDao resourceDao) {
        super(configuration);
        this.transaction = transaction;
        this.objectManager = objectManager;
        this.resourceDao = resourceDao;
    }

    @Override
    public Account getOwnerAcccountForCluster(Long clusterId) {
        return create()
                .select(ACCOUNT.fields())
                    .from(ACCOUNT)
                .where(ACCOUNT.CLUSTER_OWNER.isTrue()
                        .and(ACCOUNT.CLUSTER_ID.eq(clusterId)))
                .fetchAnyInto(AccountRecord.class);
    }

    @Override
    public Long getOwnerAcccountIdForCluster(Long clusterId) {
        return create()
                .select(ACCOUNT.ID)
                .from(ACCOUNT)
                .where(ACCOUNT.CLUSTER_OWNER.isTrue()
                        .and(ACCOUNT.CLUSTER_ID.eq(clusterId)))
                .fetchAny(ACCOUNT.ID);
    }

    @Override
    public Account createOwnerAccount(Cluster cluster) {
        return transaction.doInTransactionResult(() -> {
            Account account = resourceDao.createAndSchedule(Account.class,
                    ACCOUNT.NAME, "System",
                    ACCOUNT.EXTERNAL_ID, ProjectConstants.SYSTEM_PROJECT_EXTERNAL_ID,
                    ACCOUNT.CLUSTER_OWNER, true,
                    ACCOUNT.CLUSTER_ID, cluster.getId(),
                    ACCOUNT.KIND, ProjectConstants.TYPE);
            if (cluster.getCreatorId() != null) {
                grantOwner(cluster.getCreatorId(), ProjectConstants.RANCHER_ID, account);
            }

            return account;
        });
    }

    @Override
    public Account getDefaultProject(Cluster cluster) {
        return objectManager.findAny(Account.class,
                ACCOUNT.CLUSTER_ID, cluster.getId(),
                ACCOUNT.NAME, ServiceConstants.DEFAULT_STACK_NAME,
                ACCOUNT.KIND, ProjectConstants.TYPE,
                ACCOUNT.REMOVED, null);
    }

    @Override
    public Account createDefaultProject(Cluster cluster) {
        return transaction.doInTransactionResult(() -> {
            Map<String, Object> data = objectManager.convertToPropertiesFor(Account.class,
                    CollectionUtils.asMap(
                        ACCOUNT.CLUSTER_ID, cluster.getId(),
                        ACCOUNT.NAME, ServiceConstants.DEFAULT_STACK_NAME,
                        ACCOUNT.EXTERNAL_ID, ProjectConstants.DEFAULT_PROJECT_EXTERNAL_ID,
                        ACCOUNT.KIND, ProjectConstants.TYPE));
            data.put(AccountConstants.OPTION_CREATE_OWNER_ACCESS, true);

            return resourceDao.createAndSchedule(Account.class, data);
        });
    }

    protected void grantOwner(Object id, String idType, Account toProject) {
        objectManager.create(ProjectMember.class,
                PROJECT_MEMBER.ACCOUNT_ID, toProject.getId(),
                PROJECT_MEMBER.PROJECT_ID, toProject.getId(),
                PROJECT_MEMBER.STATE, CommonStatesConstants.ACTIVE,
                PROJECT_MEMBER.EXTERNAL_ID, id,
                PROJECT_MEMBER.EXTERNAL_ID_TYPE, idType,
                PROJECT_MEMBER.ROLE, ProjectConstants.OWNER);
    }

    @Override
    public Cluster assignTokens(Cluster cluster) {
        return transaction.doInTransactionResult(() -> {
            Long credId = DataAccessor.fieldLong(cluster, ClusterConstants.FIELD_REGISTRATION_ID);
            credId = createRegistrationCred(cluster, credId);

            return objectManager.setFields(cluster,
                    ClusterConstants.FIELD_REGISTRATION_ID, credId);
        });
    }

    @Override
    public Instance getAnyRancherAgent(Cluster cluster) {
        return create().select(INSTANCE.fields())
                .from(INSTANCE)
                .join(HOST)
                    .on(INSTANCE.HOST_ID.eq(HOST.ID))
                .join(AGENT)
                    .on(AGENT.ID.eq(HOST.AGENT_ID))
                .join(ACCOUNT)
                    .on(ACCOUNT.ID.eq(INSTANCE.ACCOUNT_ID))
                .where(ACCOUNT.CLUSTER_OWNER.isTrue()
                    .and(AGENT.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(ACCOUNT.CLUSTER_ID.eq(cluster.getId()))
                    .and(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING))
                    .and(INSTANCE.HOST_ID.isNotNull())
                    .and(INSTANCE.NAME.eq("rancher-agent")))
                .fetchAnyInto(InstanceRecord.class);
    }

    private Long createRegistrationCred(Cluster cluster, Long existingId) {
        if (existingId != null) {
            return existingId;
        }

        Account account = objectManager.findOne(Account.class,
                ACCOUNT.UUID, "register");

        String[] keys = CredentialConstants.generateKeys();

        return objectManager.create(Credential.class,
                CREDENTIAL.PUBLIC_VALUE, keys[0],
                CREDENTIAL.SECRET_VALUE, keys[1],
                CREDENTIAL.KIND, CredentialConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN,
                CREDENTIAL.STATE, CommonStatesConstants.ACTIVE,
                CredentialConstants.FIELD_CLUSTER_ID, cluster.getId(),
                CREDENTIAL.ACCOUNT_ID, account.getId()).getId();
    }
}
