package io.cattle.platform.core.dao.impl;

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
    public Account createOwnerAccount(Cluster cluster) {
        return transaction.doInTransactionResult(() -> {
            Account account = resourceDao.createAndSchedule(Account.class,
                    ACCOUNT.NAME, cluster.getName() + " System",
                    ACCOUNT.CLUSTER_OWNER, true,
                    ACCOUNT.CLUSTER_ID, cluster.getId(),
                    ACCOUNT.KIND, ProjectConstants.TYPE);
            objectManager.create(ProjectMember.class,
                    PROJECT_MEMBER.ACCOUNT_ID, account.getId(),
                    PROJECT_MEMBER.PROJECT_ID, account.getId(),
                    PROJECT_MEMBER.STATE, CommonStatesConstants.ACTIVE,
                    PROJECT_MEMBER.EXTERNAL_ID, cluster.getCreatorId(),
                    PROJECT_MEMBER.EXTERNAL_ID_TYPE, ProjectConstants.RANCHER_ID,
                    PROJECT_MEMBER.ROLE, ProjectConstants.OWNER);
            return account;
        });
    }

}
