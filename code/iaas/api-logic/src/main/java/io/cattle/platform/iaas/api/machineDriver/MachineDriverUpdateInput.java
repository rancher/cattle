package io.cattle.platform.iaas.api.machinedriver;

public interface MachineDriverUpdateInput {

    String getUri();

    String getName();

    String getmd5checksum();
}
