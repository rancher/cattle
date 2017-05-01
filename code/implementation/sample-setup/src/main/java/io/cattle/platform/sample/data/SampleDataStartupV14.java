package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.DataTable.*;

import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Data;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;


public class SampleDataStartupV14 extends AbstractSampleData {


    @Inject
    ObjectManager objectManager;
    @Inject
    DataDao dataDao;

    @Override
    protected String getName() {
        return "sampleDataVersion14";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        List<Data> datas = objectManager.find(Data.class,
                DATA.NAME, new Condition(ConditionType.LIKE, "service.v2.%.cert"));
        for (Data data : datas) {
            String name = data.getName();
            String[] parts = name.split("[.]");
            if (parts.length != 4) {
                continue;
            }

            Long id = null;
            try {
                id = Long.parseLong(parts[2]);
            } catch (NumberFormatException nfe) {
                continue;
            }

            Service service = objectManager.loadResource(Service.class, id);
            if (service == null || service.getRemoved() != null || StringUtils.isBlank(service.getName())) {
                continue;
            }
            String newKey = "service.v3." + service.getAccountId() + "." + service.getName() + ".cert";
            dataDao.getOrCreate(newKey, false, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return data.getValue();
                }
            });
        }
    }

}