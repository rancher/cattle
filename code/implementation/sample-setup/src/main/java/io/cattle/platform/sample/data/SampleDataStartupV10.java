package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.DynamicSchemaRoleTable.*;
import static io.cattle.platform.core.model.tables.DynamicSchemaTable.*;
import static io.cattle.platform.core.model.tables.MachineDriverTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.MachineDriverConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.MachineDriver;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DefaultDSLContext;

import com.netflix.config.DynamicStringListProperty;

public class SampleDataStartupV10 extends AbstractSampleData {

    @Inject
    Configuration configuration;

    private static final DynamicStringListProperty DRIVERS = ArchaiusUtil.getList("machine.drivers.default");

    protected DSLContext create() {
        return new DefaultDSLContext(configuration);
    }

    @Override
    protected String getName() {
        return "sampleDataVersion10";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        create().delete(DYNAMIC_SCHEMA_ROLE).execute();
        create().delete(DYNAMIC_SCHEMA).execute();
        create().delete(MACHINE_DRIVER).where(
                MACHINE_DRIVER.STATE.ne(CommonStatesConstants.ACTIVE)).execute();

        Set<String> existing = new HashSet<>();
        for (MachineDriver md : objectManager.find(MachineDriver.class, ObjectMetaDataManager.STATE_FIELD, CommonStatesConstants.ACTIVE)) {
            existing.add(md.getName());
            objectManager.setFields(md,
                    ObjectMetaDataManager.STATE_FIELD, CommonStatesConstants.INACTIVE,
                    MachineDriverConstants.FIELD_DEFAULT_ACTIVE, true,
                    MachineDriverConstants.FIELD_CHECKSUM, md.getMd5checksum(),
                    MachineDriverConstants.FIELD_URL, md.getUri());
        }

        for (String name : DRIVERS.get()) {
            if (existing.contains(name)) {
                continue;
            }

            List<String> values = ArchaiusUtil.getList(String.format("machine.driver.%s", name)).get();
            String activate = values.get(0);
            String url = values.get(1);
            String checksum = "";
            if (values.size() > 2) {
                checksum = values.get(2);
            }
            String uuid = Long.toString((url+checksum+name).hashCode());
            boolean activateDefault = Boolean.parseBoolean(activate);
            boolean builtin = url.equals("local://");

            createByUuid(MachineDriver.class, uuid,
                MACHINE_DRIVER.KIND, MachineDriverConstants.TYPE,
                ObjectMetaDataManager.NAME_FIELD, name,
                MachineDriverConstants.FIELD_BUILTIN, builtin,
                MachineDriverConstants.FIELD_CHECKSUM, checksum,
                MachineDriverConstants.FIELD_URL, url,
                MachineDriverConstants.FIELD_DEFAULT_ACTIVE, activateDefault,
                MACHINE_DRIVER.STATE, CommonStatesConstants.INACTIVE);
        }
    }

}