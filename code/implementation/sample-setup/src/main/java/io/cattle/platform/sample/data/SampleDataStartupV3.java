package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.AccountTable.ACCOUNT;
import io.cattle.platform.core.model.Account;

import java.util.List;

import javax.inject.Inject;

public class SampleDataStartupV3 extends AbstractSampleData {

    @Inject
    SampleDataStartupV1 v1;

    @Override
    protected String getName() {
        return "sampleDataVersion3";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        v1.start();

        toCreate.add(createByUuid(Account.class, "token",
                ACCOUNT.KIND, "token",
                ACCOUNT.NAME, "token"));

    }

}