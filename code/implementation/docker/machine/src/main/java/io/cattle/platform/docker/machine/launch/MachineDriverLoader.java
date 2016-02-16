package io.cattle.platform.docker.machine.launch;

import static io.cattle.platform.core.model.tables.MachineDriverTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.MachineDriver;
import io.cattle.platform.core.model.tables.records.MachineDriverRecord;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.InitializationTask;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringProperty;

public class MachineDriverLoader implements InitializationTask {

    public static final DynamicStringProperty CONFIG_FILE = ArchaiusUtil.getString("machine.driver.config");

    private static final Logger log = LoggerFactory.getLogger(MachineDriverLoader.class);

    @Inject
    LockManager lockManager;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public void start() {
        // do logic once
        loadDrivers();
        CONFIG_FILE.addCallback(new Runnable() {
            @Override
            public void run() {
                loadDrivers();
            }
        });
    }

    public void loadDrivers() {
        lockManager.lock(new MachineDriverLoaderLock(), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                String configLocation = CONFIG_FILE.get();
                if (StringUtils.isBlank(configLocation)) {
                    return;
                }

                InputStream in = null;
                byte[] configJson = null;
                DriverConfig dc = null;
                try {
                    URL configUrl = new URL(configLocation);
                    in = configUrl.openStream();
                    configJson = IOUtils.toByteArray(in);
                    dc = jsonMapper.readValue(configJson, DriverConfig.class);
                } catch (Exception e) {
                    log.error("Error while reading machine driver config json.", e);
                    return;
                } finally {
                    IOUtils.closeQuietly(in);
                }
                
                if (dc.getDrivers() == null || dc.getDrivers().isEmpty()) {
                    return;
                }
                
                List<MachineDriver> driverList = objectManager.find(MachineDriver.class, MACHINE_DRIVER.REMOVED, (Object)null);
                Map<String, MachineDriver> existingDrivers = new HashMap<String, MachineDriver>();
                for (MachineDriver d : driverList) {
                    existingDrivers.put(d.getName(), d);
                }

                for (MachineDriver newDriver : dc.getDrivers()) {
                    if (StringUtils.isBlank(newDriver.getName()) || StringUtils.isBlank(newDriver.getUri())) {
                        log.error("Machine driver invalid. Name and uri must not be blank. Name: {}, Uri: {}", newDriver.getName(), newDriver.getUri());
                        continue;
                    }

                    if (existingDrivers.containsKey(newDriver.getName())) {
                        MachineDriver existing = existingDrivers.get(newDriver.getName());
                        if (!StringUtils.equals(existing.getMd5checksum(), newDriver.getMd5checksum())
                                || !StringUtils.equals(existing.getUri(), newDriver.getUri())) {
                            processManager.executeStandardProcess(StandardProcess.REMOVE, existing, null);
                        } else {
                            continue;
                        }
                    }
                    log.info("Loading machine driver with name {}, uri {}, checksum {}", newDriver.getName(), newDriver.getUri(), newDriver.getMd5checksum());
                    objectManager.create(newDriver);
                    processManager.scheduleStandardProcess(StandardProcess.CREATE, newDriver, null);
                }
            }
        });
    }

    @Override
    public void stop() {
        // Nothing to stop
    }
}

class DriverConfig {
    List<MachineDriverRecord> drivers;

    public List<MachineDriverRecord> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<MachineDriverRecord> drivers) {
        this.drivers = drivers;
    }
}