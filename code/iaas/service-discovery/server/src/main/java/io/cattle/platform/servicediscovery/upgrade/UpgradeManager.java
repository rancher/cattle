package io.cattle.platform.servicediscovery.upgrade;

import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.RollingRestartStrategy;
import io.cattle.platform.core.model.Service;

public interface UpgradeManager {

    void upgrade(Service service, InServiceUpgradeStrategy strategy, String currentProcess, boolean sleep,
            boolean prepullImages);

    void rollback(Service service, InServiceUpgradeStrategy strategy);

    void finishUpgrade(Service service, boolean reconcile);

    void restart(Service service, RollingRestartStrategy strategy);

}
