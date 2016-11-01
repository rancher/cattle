package io.cattle.platform.ha.monitor.impl;

import static io.cattle.platform.core.constants.CommonStatesConstants.*;
import static io.cattle.platform.core.constants.ContainerEventConstants.*;
import static io.cattle.platform.core.constants.InstanceConstants.*;
import static org.junit.Assert.*;

import io.cattle.platform.ha.monitor.model.KnownInstance;
import io.cattle.platform.object.meta.impl.DefaultObjectMetaDataManager;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class PingInstancesMonitorImplTest {

    PingInstancesMonitorImpl monitor;
    Map<String, KnownInstance> knownInstances;
    Map<String, ReportedInstance> needsSynced;
    Map<String, String> syncActions;
    ReportedInstances reportedInstances;
    static final Set<String> restingStates = new HashSet<String>();
    static {
        restingStates.add(STATE_RUNNING);
        restingStates.add(STATE_CREATED);
        restingStates.add(STATE_STOPPED);
        restingStates.add(REMOVED);
    }

    @Before
    public void setup() {
        monitor = new PingInstancesMonitorImpl();
        monitor.objectMetaDataManager = new DefaultObjectMetaDataManager() {
            @Override
            public boolean isTransitioningState(Class<?> resourceType, String state) {
                return !restingStates.contains(state);
            }
        };
        knownInstances = new HashMap<String, KnownInstance>();
        needsSynced = new HashMap<String, ReportedInstance>();
        syncActions = new HashMap<String, String>();
        reportedInstances = new ReportedInstances();
    }

    String setupSync(String key, boolean byExternalId, String knownInstanceState,
            String reportedInstanceState) {
        return setupSync(key, byExternalId, null, knownInstanceState, reportedInstanceState);
    }
    String setupSync(String key, boolean byExternalId, String instanceTriggeredStop, String knownInstanceState,
            String reportedInstanceState) {
        key = fullKey(key, byExternalId);
        String uuid = key + "-uuid";
        String externalId = key + "-extid";

        String instaceEid = byExternalId ? externalId : null;
        Date instanceRemoved = REMOVED.equals(knownInstanceState) ? new Date() : null;
        String reportedUuid = !byExternalId ? uuid : "garbage-" + key;

        if (knownInstanceState != null)
            addKnownInstance(uuid, instaceEid, knownInstanceState, instanceRemoved);
        if (reportedInstanceState != null)
            addReportedInstance(reportedUuid, externalId, reportedInstanceState);

        return externalId;
    }

    String fullKey(String key, boolean byExternalId) {
        StringBuilder fullKey = new StringBuilder();
        fullKey.append("user-");

        fullKey.append(key);

        if (byExternalId)
            fullKey.append("-byext");
        else
            fullKey.append("-byuuid");

        return fullKey.toString();
    }

    String uuid(String key, boolean byExternalId) {
        String fullKey = fullKey(key, byExternalId);
        return fullKey + "-uuid";
    }

    @Test
    public void testDetermineSyncActions() {
        // container running in rancher, stopped on host
        String externalIdA = setupSync("rancher-running-host-stopped", false, STATE_RUNNING, STATE_STOPPED);
        String externalIdC = setupSync("rancher-running-host-stopped", true, STATE_RUNNING, STATE_STOPPED);

        // Container running in rancher, not on host
        String externalIdD = setupSync("rancher-running-host-destroyed", false, STATE_RUNNING, null);
        String externalIdE = setupSync("rancher-running-host-destroyed", true, STATE_RUNNING, null);

        // Container stopped in rancher, running on host
        String externalIdH = setupSync("rancher-stopped-host-running", false, STATE_STOPPED, STATE_RUNNING);
        String externalIdI = setupSync("rancher-stopped-host-running", true, STATE_STOPPED, STATE_RUNNING);

        // Container stopped in rancher, destroyed on host
        String externalIdK = setupSync("rancher-stopped-host-destroyed", false, STATE_STOPPED, null);
        String externalIdL = setupSync("rancher-stopped-host-destroyed", true, STATE_STOPPED, null);
        String uuidK = uuid("rancher-stopped-host-destroyed", false);

        // Container removed in rancher, running on host. Action: force stop
        String externalIdO = setupSync("rancher-removed-host-running", false, REMOVED, STATE_RUNNING);
        String externalIdP = setupSync("rancher-removed-host-running", true, REMOVED, STATE_RUNNING);

        // Container removed in rancher, stopped on host. Action: do nothing
        String externalIdS = setupSync("rancher-removed-host-stopped", false, REMOVED, STATE_STOPPED);
        String externalIdT = setupSync("rancher-removed-host-stopped", true, REMOVED, STATE_STOPPED);
        String uuidS = uuid("rancher-removed-host-stopped", false);
        String uuidT = uuid("rancher-removed-host-stopped", false);

        // Container removed in rancher, removed on host, by uuid. Action: do nothing
        String externalIdW = setupSync("rancher-removed-host-removed", false, REMOVED, null);
        String externalIdX = setupSync("rancher-removed-host-removed", true, REMOVED, null);
        String uuidW = uuid("rancher-removed-host-stopped", false);
        String uuidX = uuid("rancher-removed-host-stopped", false);

        // Container doesn't exist in rancher, running on host
        String externalIdAA = setupSync("rancher-notexist-host-running", false, null, STATE_RUNNING); // no-op start
        String externalIdBB = setupSync("rancher-notexist-host-running", true, null, STATE_RUNNING); // no-op start

        // Container doesn't exist in rancher, stopped on host. User containers will be created via no-op start and be stopped in subsequent ping
        String externalIdEE = setupSync("rancher-notexist-host-stopped", false, null, STATE_STOPPED); // no-op start
        String externalIdFF = setupSync("rancher-notexist-host-stopped", true, null, STATE_STOPPED); // no-op start

        monitor.determineSyncActions(knownInstances, reportedInstances, needsSynced, syncActions, false);

        assertSyncAction(externalIdA, EVENT_STOP);
        assertSyncAction(externalIdC, EVENT_STOP);

        assertTrue(!needsSynced.containsKey(externalIdD));
        assertSyncAction(externalIdE, EVENT_DESTROY);

        assertSyncAction(externalIdH, EVENT_START);
        assertSyncAction(externalIdI, EVENT_START);

        assertDoNothing(externalIdK, uuidK);
        assertSyncAction(externalIdL, EVENT_DESTROY);

        assertSyncAction(externalIdO, EVENT_INSTANCE_FORCE_STOP);
        assertSyncAction(externalIdP, EVENT_INSTANCE_FORCE_STOP);

        assertDoNothing(externalIdS, uuidS);
        assertDoNothing(externalIdT, uuidT);

        assertDoNothing(externalIdW, uuidW);
        assertDoNothing(externalIdX, uuidX);

        assertSyncAction(externalIdAA, EVENT_START);
        assertSyncAction(externalIdBB, EVENT_START);

        assertSyncAction(externalIdEE, EVENT_START);
        assertSyncAction(externalIdFF, EVENT_START);
    }

    void assertDoNothing(String externalId, String uuid) {
        assertTrue(!needsSynced.containsKey(externalId));
    }

    void assertSyncAction(String externalId, String action) {
        assertTrue(needsSynced.containsKey(externalId));
        assertEquals(action, syncActions.get(externalId));
    }

    ReportedInstance addReportedInstance(String uuid, String externalId, String state) {
        ReportedInstance ri = new ReportedInstance();
        ri.setExternalId(externalId);
        ri.setUuid(uuid);
        ri.setState(state);
        reportedInstances.byExternalId.put(externalId, ri);
        reportedInstances.byUuid.put(uuid, ri);
        return ri;
    }

    KnownInstance addKnownInstance(String uuid, String externalId, String state, Date removed) {
        KnownInstance ki = new KnownInstance();
        ki.setExternalId(externalId);
        ki.setUuid(uuid);
        ki.setState(state);
        ki.setRemoved(removed);

        knownInstances.put(uuid, ki);
        return ki;
    }
}
