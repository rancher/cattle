package io.cattle.platform.process.snapshot;

import javax.inject.Named;

import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.SnapshotStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

@Named
public class SnapshotRemove extends AbstractDefaultProcessHandler {

	@Override
	public HandlerResult handle(ProcessState state, ProcessInstance process) {
		final Snapshot snapshot = (Snapshot)state.getResource();

		return null;
	}

}
