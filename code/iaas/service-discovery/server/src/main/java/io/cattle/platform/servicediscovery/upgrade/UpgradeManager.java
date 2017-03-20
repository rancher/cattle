package io.cattle.platform.servicediscovery.upgrade;

import io.cattle.platform.core.addon.RollingRestartStrategy;
import io.cattle.platform.core.model.Service;

public interface UpgradeManager {

    void upgrade(Service service, String currentProcess, boolean sleep, boolean prepullImages);

    void finishUpgrade(Service service, boolean reconcile);

    void restart(Service service, RollingRestartStrategy strategy);

}
