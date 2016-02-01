package io.cattle.platform.iaas.api.machine.driver;

public interface MachineDriverUpdateInput {

    String getUri();

    String getName();

    String getmd5checksum();
}
