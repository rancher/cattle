package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public interface SetComputeFlavorInput {

    String getComputeFlavor();

}
