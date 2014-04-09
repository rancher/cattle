package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;

public interface NicDao {

    Nic getPrimaryNic(Instance instance);

}
