package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.ProjectMemberTable.*;

import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectMember;

import java.util.List;

public class SampleDataStartupV3 extends AbstractSampleData {

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
