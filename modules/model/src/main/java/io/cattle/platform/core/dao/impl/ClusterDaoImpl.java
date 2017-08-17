package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.addon.K8sClientConfig;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.core.model.tables.records.AccountRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.jooq.Configuration;

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
    public Account getOwnerAcccountForCluster(Cluster cluster) {
        return create()
                .select(ACCOUNT.fields())
                    .from(ACCOUNT)
                .where(ACCOUNT.CLUSTER_OWNER.isTrue()
                    .and(ACCOUNT.CLUSTER_ID.eq(cluster.getId())))
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
                    ACCOUNT.CLUSTER_OWNER, true,
                    ACCOUNT.CLUSTER_ID, cluster.getId(),
                    ACCOUNT.KIND, ProjectConstants.TYPE);
            if (cluster.getCreatorId() != null) {
                grantOwner(cluster.getCreatorId(), ProjectConstants.RANCHER_ID, account);
            }
            return account;
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
    public Cluster createClusterForAccount(Account account, K8sClientConfig clientConfig) {
        return transaction.doInTransactionResult(() -> {
            Cluster cluster = resourceDao.createAndSchedule(Cluster.class,
                    CLUSTER.NAME, account.getName() + "-cluster",
                    CLUSTER.EMBEDDED, clientConfig == null ? true : false,
                    ClusterConstants.FIELD_K8S_CLIENT_CONFIG, clientConfig);

            Account systemEnv = createOwnerAccount(cluster);

            for (ProjectMember member : objectManager.find(ProjectMember.class,
                    PROJECT_MEMBER.PROJECT_ID, account.getId(),
                    PROJECT_MEMBER.ROLE, ProjectConstants.OWNER,
                    PROJECT_MEMBER.STATE, CommonStatesConstants.ACTIVE)) {
                grantOwner(member.getExternalId(), member.getExternalIdType(), systemEnv);
            }

            objectManager.setFields(account,
                    ACCOUNT.CLUSTER_ID, cluster.getId());

            return cluster;
        });
    }

}
