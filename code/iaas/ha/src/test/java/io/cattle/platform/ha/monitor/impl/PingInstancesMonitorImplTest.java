package io.cattle.platform.ha.monitor.impl;

import static io.cattle.platform.core.constants.CommonStatesConstants.*;
import static io.cattle.platform.core.constants.ContainerEventConstants.*;
import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.process.instance.InstanceProcessOptions.*;
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
    Set<String> needsHaRestart;
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
            public boolean isTransitioningState(Class<?> resourceType, String state) {
                return !restingStates.contains(state);
            }
        };
        knownInstances = new HashMap<String, KnownInstance>();
        needsSynced = new HashMap<String, ReportedInstance>();
        syncActions = new HashMap<String, String>();
        needsHaRestart = new HashSet<String>();
        reportedInstances = new ReportedInstances();
    }

    String setupSync(String key, boolean byExternalId, boolean sysCon, String knownInstanceState, 
            String reportedInstanceState) {
        return setupSync(key, byExternalId, sysCon, null, knownInstanceState, reportedInstanceState);
    }
    String setupSync(String key, boolean byExternalId, boolean sysCon, String instanceTriggeredStop, String knownInstanceState, 
            String reportedInstanceState) {
        key = fullKey(key, byExternalId, sysCon);
        String uuid = key + "-uuid";
        String externalId = key + "-extid";

        String instaceEid = byExternalId ? externalId : null;
        String instanceSysCon = sysCon ? "ImASysCon" : null;
        Date instanceRemoved = REMOVED.equals(knownInstanceState) ? new Date() : null;
        String reportedUuid = !byExternalId ? uuid : "garbage-" + key;

        if (knownInstanceState != null)
            addKnownInstance(uuid, instaceEid, knownInstanceState, instanceSysCon, instanceRemoved, instanceTriggeredStop);
        if (reportedInstanceState != null)
            addReportedInstance(reportedUuid, externalId, reportedInstanceState, sysCon);

        return externalId;
    }

    String fullKey(String key, boolean byExternalId, boolean sysCon) {
        StringBuilder fullKey = new StringBuilder();
        if (sysCon)
            fullKey.append("syscon-");
        else
            fullKey.append("user-");

        fullKey.append(key);

        if (byExternalId)
            fullKey.append("-byext");
        else
            fullKey.append("-byuuid");

        return fullKey.toString();
    }

    String uuid(String key, boolean byExternalId, boolean sysCon) {
        String fullKey = fullKey(key, byExternalId, sysCon);
        return fullKey + "-uuid";
    }

    @Test
    public void testDetermineSyncActions() {
        // container running in rancher, stopped on host
        String externalIdA = setupSync("rancher-running-host-stopped", false, false, STATE_RUNNING, STATE_STOPPED);
        String externalIdC = setupSync("rancher-running-host-stopped", true, false, STATE_RUNNING, STATE_STOPPED);
        String externalIdB = setupSync("rancher-running-host-stopped", false, true, STATE_RUNNING, STATE_STOPPED);
        String externalIdC1 = setupSync("rancher-running-host-stopped", true, true, STATE_RUNNING, STATE_STOPPED);
        String externalIdC2 = setupSync("rancher-running-host-stopped-triggered", false, false, "restart", STATE_RUNNING, STATE_STOPPED);
        String externalIdC3 = setupSync("rancher-running-host-stopped-triggered", true, false, "restart", STATE_RUNNING, STATE_STOPPED);

        // Container running in rancher, not on host
        String externalIdD = setupSync("rancher-running-host-destroyed", false, false, STATE_RUNNING, null);
        String externalIdE = setupSync("rancher-running-host-destroyed", true, false, STATE_RUNNING, null);
        String externalIdF = setupSync("rancher-running-host-destroyed", false, true, STATE_RUNNING, null);
        String externalIdG = setupSync("rancher-running-host-destroyed", true, true, STATE_RUNNING, null); // sysCon: ha restart
        String uuidD = uuid("rancher-running-host-destroyed", false, false);
        String uuidF = uuid("rancher-running-host-destroyed", false, true);
        String uuidG = uuid("rancher-running-host-destroyed", true, true);

        // Container stopped in rancher, running on host
        String externalIdH = setupSync("rancher-stopped-host-running", false, false, STATE_STOPPED, STATE_RUNNING);
        String externalIdI = setupSync("rancher-stopped-host-running", true, false, STATE_STOPPED, STATE_RUNNING);
        String externalIdJ = setupSync("rancher-stopped-host-running", false, true, STATE_STOPPED, STATE_RUNNING);
        String externalIdJ1 = setupSync("rancher-stopped-host-running", true, true, STATE_STOPPED, STATE_RUNNING);

        // Container stopped in rancher, destroyed on host, by uuid
        String externalIdK = setupSync("rancher-stopped-host-destroyed", false, false, STATE_STOPPED, null);
        String externalIdL = setupSync("rancher-stopped-host-destroyed", true, false, STATE_STOPPED, null);
        String externalIdM = setupSync("rancher-stopped-host-destroyed", false, true, STATE_STOPPED, null);
        String externalIdN = setupSync("rancher-stopped-host-destroyed", true, true, STATE_STOPPED, null);
        String uuidK = uuid("rancher-stopped-host-destroyed", false, false);
        String uuidL = uuid("rancher-stopped-host-destroyed", true, false);
        String uuidM = uuid("rancher-stopped-host-destroyed", false, true);
        String uuidN = uuid("rancher-stopped-host-destroyed", true, true);
        
        // Container removed in rancher, running on host. Action: force stop
        String externalIdO = setupSync("rancher-removed-host-running", false, false, REMOVED, STATE_RUNNING);
        String externalIdP = setupSync("rancher-removed-host-running", true, false, REMOVED, STATE_RUNNING);
        String externalIdQ = setupSync("rancher-removed-host-running", false, true, REMOVED, STATE_RUNNING);
        String externalIdR = setupSync("rancher-removed-host-running", true, true, REMOVED, STATE_RUNNING);

        // Container removed in rancher, stopped on host. Action: do nothing
        String externalIdS = setupSync("rancher-removed-host-stopped", false, false, REMOVED, STATE_STOPPED);
        String externalIdT = setupSync("rancher-removed-host-stopped", true, false, REMOVED, STATE_STOPPED);
        String externalIdU = setupSync("rancher-removed-host-stopped", false, true, REMOVED, STATE_STOPPED);
        String externalIdV = setupSync("rancher-removed-host-stopped", true, true, REMOVED, STATE_STOPPED);
        String uuidS = uuid("rancher-removed-host-stopped", false, false);
        String uuidT = uuid("rancher-removed-host-stopped", false, false);
        String uuidU = uuid("rancher-removed-host-stopped", false, false);
        String uuidV = uuid("rancher-removed-host-stopped", false, false);

        // Container removed in rancher, removed on host, by uuid. Action: do nothing
        String externalIdW = setupSync("rancher-removed-host-removed", false, false, REMOVED, null);
        String externalIdX = setupSync("rancher-removed-host-removed", true, false, REMOVED, null);
        String externalIdY = setupSync("rancher-removed-host-removed", false, true, REMOVED, null);
        String externalIdZ = setupSync("rancher-removed-host-removed", true, true, REMOVED, null);
        String uuidW = uuid("rancher-removed-host-stopped", false, false);
        String uuidX = uuid("rancher-removed-host-stopped", false, false);
        String uuidY = uuid("rancher-removed-host-stopped", false, false);
        String uuidZ = uuid("rancher-removed-host-stopped", false, false);

        // Container doesn't exist in rancher, running on host
        String externalIdAA = setupSync("rancher-notexist-host-running", false, false, null, STATE_RUNNING); // no-op create
        String externalIdBB = setupSync("rancher-notexist-host-running", true, false, null, STATE_RUNNING); // no-op create
        String externalIdCC = setupSync("rancher-notexist-host-running", false, true, null, STATE_RUNNING); // sysCon: force stop
        String externalIdDD = setupSync("rancher-notexist-host-running", true, true, null, STATE_RUNNING); // sysCon: force stop

        // Container doesn't exist in rancher, stopped on host
        String externalIdEE = setupSync("rancher-notexist-host-stopped", false, false, null, STATE_STOPPED); // no-op create
        String externalIdFF = setupSync("rancher-notexist-host-stopped", true, false, null, STATE_STOPPED); // no-op create
        String externalIdGG = setupSync("rancher-notexist-host-stopped", false, true, null, STATE_STOPPED); // sysCon: do nothing
        String uuidGG = uuid("rancher-notexist-host-stopped", false, true);
        String externalIdHH = setupSync("rancher-notexist-host-stopped", true, true, null, STATE_STOPPED); // sysCon: do nothing
        String uuidHH = uuid("rancher-notexist-host-stopped", true, true);

        monitor.determineSyncActions(knownInstances, reportedInstances, needsSynced, syncActions, needsHaRestart, false);

        assertSyncAction(externalIdA, EVENT_STOP);
        assertSyncAction(externalIdC, EVENT_STOP);
        assertSyncAction(externalIdB, HA_RESTART);
        assertSyncAction(externalIdC1, HA_RESTART);
        assertSyncAction(externalIdC2, HA_RESTART);
        assertSyncAction(externalIdC3, HA_RESTART);
        
        assertTrue(needsHaRestart.contains(uuidD));
        assertTrue(!needsSynced.containsKey(externalIdD));
        assertSyncAction(externalIdE, EVENT_STOP);
        assertTrue(needsHaRestart.contains(uuidF));
        assertTrue(!needsSynced.containsKey(externalIdF));
        assertTrue(needsHaRestart.contains(uuidG));
        assertTrue(!needsSynced.containsKey(externalIdG));

        assertSyncAction(externalIdH, EVENT_START);
        assertSyncAction(externalIdI, EVENT_START);
        assertSyncAction(externalIdJ, EVENT_INSTANCE_FORCE_STOP);
        assertSyncAction(externalIdJ1, EVENT_INSTANCE_FORCE_STOP);
        
        assertDoNothing(externalIdK, uuidK);
        assertDoNothing(externalIdL, uuidL);
        assertDoNothing(externalIdM, uuidM);
        assertDoNothing(externalIdN, uuidN);
        
        assertSyncAction(externalIdO, EVENT_INSTANCE_FORCE_STOP);
        assertSyncAction(externalIdP, EVENT_INSTANCE_FORCE_STOP);
        assertSyncAction(externalIdQ, EVENT_INSTANCE_FORCE_STOP);
        assertSyncAction(externalIdR, EVENT_INSTANCE_FORCE_STOP);
        
        assertDoNothing(externalIdS, uuidS);
        assertDoNothing(externalIdT, uuidT);
        assertDoNothing(externalIdU, uuidU);
        assertDoNothing(externalIdV, uuidV);
        
        assertDoNothing(externalIdW, uuidW);
        assertDoNothing(externalIdX, uuidX);
        assertDoNothing(externalIdY, uuidY);
        assertDoNothing(externalIdZ, uuidZ);
        
        assertSyncAction(externalIdAA, EVENT_CREATE);
        assertSyncAction(externalIdBB, EVENT_CREATE);
        assertSyncAction(externalIdCC, EVENT_INSTANCE_FORCE_STOP);
        assertSyncAction(externalIdDD, EVENT_INSTANCE_FORCE_STOP);
        
        assertSyncAction(externalIdEE, EVENT_CREATE);
        assertSyncAction(externalIdFF, EVENT_CREATE);
        assertDoNothing(externalIdGG, uuidGG);
        assertDoNothing(externalIdHH, uuidHH);
    }

    void assertDoNothing(String externalId, String uuid) {
        assertTrue(!needsSynced.containsKey(externalId));
        assertTrue(!needsHaRestart.contains(uuid));
    }

    void assertSyncAction(String externalId, String action) {
        assertTrue(needsSynced.containsKey(externalId));
        assertEquals(action, syncActions.get(externalId));
    }

    ReportedInstance addReportedInstance(String uuid, String externalId, String state, boolean sysCon) {
        ReportedInstance ri = new ReportedInstance();
        ri.setExternalId(externalId);
        ri.setUuid(uuid);
        ri.setState(state);
        if (sysCon)
            ri.getLabels().put(LABEL_RANCHER_SYSTEM_CONTAINER, "imASysCon");
        reportedInstances.byExternalId.put(externalId, ri);
        reportedInstances.byUuid.put(uuid, ri);
        return ri;
    }

    KnownInstance addKnownInstance(String uuid, String externalId, String state) {
        return addKnownInstance(uuid, externalId, state, null, null, null);
    }

    KnownInstance addKnownInstance(String uuid, String externalId, String state, String systemContainer, Date removed, String instanceTriggeredStop) {
        KnownInstance ki = new KnownInstance();
        ki.setExternalId(externalId);
        ki.setUuid(uuid);
        ki.setState(state);
        ki.setSystemContainer(systemContainer);
        ki.setRemoved(removed);
        ki.setInstanceTriggeredStop(instanceTriggeredStop);

        knownInstances.put(uuid, ki);
        return ki;
    }
}
