package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.AccountTable.ACCOUNT;
import static io.cattle.platform.core.model.tables.ProjectMemberTable.PROJECT_MEMBER;

import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectMember;

import java.util.List;

import javax.inject.Inject;

public class SampleDataStartupV3 extends AbstractSampleData {

    SampleDataStartupV2 v2;

    @Override
    protected String getName() {
        return "sampleDataVersion3";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        v2.start();

        toCreate.add(createByUuid(Account.class, "token", ACCOUNT.KIND, "token", ACCOUNT.NAME, "token"));
        Account adminProject = createByUuid(Account.class, "adminProject", ACCOUNT.KIND, "project", ACCOUNT.NAME, "default project");
        toCreate.add(adminProject);
        toCreate.add(createByUuid(ProjectMember.class, "adminMember", PROJECT_MEMBER.ACCOUNT_ID,
                adminProject.getId(), PROJECT_MEMBER.PROJECT_ID, adminProject.getId(),
                PROJECT_MEMBER.EXTERNAL_ID, 1, PROJECT_MEMBER.EXTERNAL_ID_TYPE,
                ProjectConstants.RANCHER_ID, PROJECT_MEMBER.ROLE, "owner"));


    }

    public SampleDataStartupV2 getV2() {
        return v2;
    }

    @Inject
    public void setV2(SampleDataStartupV2 v2) {
        this.v2 = v2;
    }
}
