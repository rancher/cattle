package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.ProjectMemberTable.*;

import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.List;

public class SampleDataStartupV3 extends AbstractSampleData {

    public SampleDataStartupV3(ObjectManager objectManager, ObjectProcessManager processManager, AccountDao accountDao, JsonMapper jsonMapper,
            LockManager lockManager) {
        super(objectManager, processManager, accountDao, jsonMapper, lockManager);
    }

    @Override
    protected String getName() {
        return "sampleDataVersion3";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        toCreate.add(createByUuid(Account.class, "token", ACCOUNT.KIND, "token", ACCOUNT.NAME, "token"));
        Account adminProject = createByUuid(Account.class, "adminProject",
                ACCOUNT.KIND, "project",
                ACCOUNT.NAME, "Default",
                "orchestration", "cattle");
        toCreate.add(adminProject);
        toCreate.add(createByUuid(ProjectMember.class, "adminMember", PROJECT_MEMBER.ACCOUNT_ID,
                adminProject.getId(), PROJECT_MEMBER.PROJECT_ID, adminProject.getId(),
                PROJECT_MEMBER.EXTERNAL_ID, 1, PROJECT_MEMBER.EXTERNAL_ID_TYPE,
                ProjectConstants.RANCHER_ID, PROJECT_MEMBER.ROLE, ProjectConstants.OWNER));


    }
}
