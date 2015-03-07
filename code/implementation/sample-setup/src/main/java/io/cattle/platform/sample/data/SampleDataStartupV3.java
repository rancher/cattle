package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.AccountTable.ACCOUNT;
import io.cattle.platform.core.model.Account;

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

    }

    public SampleDataStartupV2 getV2() {
        return v2;
    }

    @Inject
    public void setV2(SampleDataStartupV2 v2) {
        this.v2 = v2;
    }
}
