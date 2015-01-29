package io.cattle.platform.process.snapshot;

import javax.inject.Inject;
import javax.inject.Named;

import static io.cattle.platform.core.model.tables.SnapshotStoragePoolMapTable.*;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.SnapshotStoragePoolMap;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

@Named
public class SnapshotBackup extends AbstractDefaultProcessHandler {

	GenericMapDao mapDao;
	ObjectManager objectManager;
	LockManager lockManager;

	@Override
	public HandlerResult handle(ProcessState state, ProcessInstance process) {
		final Snapshot snapshot = (Snapshot)state.getResource();
		
		return null;
	}

}
