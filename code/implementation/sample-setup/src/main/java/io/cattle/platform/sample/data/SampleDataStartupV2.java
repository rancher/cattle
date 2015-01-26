package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.NetworkServiceTable.NETWORK_SERVICE;
import static io.cattle.platform.core.model.tables.NetworkTable.NETWORK;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;

import java.util.List;

import javax.inject.Inject;

public class SampleDataStartupV2 extends AbstractSampleData {

    SampleDataStartupV1 v1;

    @Override
    protected String getName() {
        return "sampleDataVersion2";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        v1.start();

        Network network = objectManager.findAny(Network.class, NETWORK.UUID, "unmanaged");
        if ( network == null ) {
            return;
        }

        toCreate.add(createByUuid(NetworkService.class, "unmanaged-docker0-metadata-service",
                NETWORK_SERVICE.ACCOUNT_ID, system.getId(),
                NETWORK_SERVICE.KIND, NetworkServiceConstants.KIND_METADATA,
                NETWORK_SERVICE.NAME, "Meta data service for unmanaged docker0",
                NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NetworkServiceConstants.FIELD_CONFIG_DRIVE, true,
                NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));
        
    }

    public SampleDataStartupV1 getV1() {
        return v1;
    }

    @Inject
    public void setV1(SampleDataStartupV1 v1) {
        this.v1 = v1;
    }

}
