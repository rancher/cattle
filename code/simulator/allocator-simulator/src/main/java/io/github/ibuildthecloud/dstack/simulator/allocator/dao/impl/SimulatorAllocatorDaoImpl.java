package io.github.ibuildthecloud.dstack.simulator.allocator.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.HostTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.ImageStoragePoolMapTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.ImageTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.InstanceTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.StoragePoolHostMapTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.StoragePoolTable.*;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.simulator.allocator.dao.SimulatorAllocatorDao;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.jooq.Cursor;
import org.jooq.Record2;

public class SimulatorAllocatorDaoImpl extends AbstractJooqDao implements SimulatorAllocatorDao {

    ObjectManager objectManager;

    @Override
    public boolean isSimulatedInstance(long instanceId) {
        return create().select(STORAGE_POOL.fields())
                .from(STORAGE_POOL)
                .join(IMAGE_STORAGE_POOL_MAP)
                    .on(STORAGE_POOL.ID.eq(IMAGE_STORAGE_POOL_MAP.STORAGE_POOL_ID))
                .join(IMAGE)
                    .on(IMAGE.ID.eq(IMAGE_STORAGE_POOL_MAP.IMAGE_ID))
                .join(INSTANCE)
                    .on(INSTANCE.IMAGE_ID.eq(IMAGE.ID))
                .where(
                    INSTANCE.ID.eq(instanceId)
                    .and(IMAGE_STORAGE_POOL_MAP.REMOVED.isNull())
                    .and(STORAGE_POOL.KIND.eq("sim")))
                .fetch().size() > 0;
    }

    @Override
    public Iterator<AllocationCandidate> iteratorPools(List<Long> volumes) {
        return iteratorHosts(volumes, false);
    }

    @Override
    public Iterator<AllocationCandidate> iteratorHosts(List<Long> volumes) {
        return iteratorHosts(volumes, true);
    }

    protected Iterator<AllocationCandidate> iteratorHosts(List<Long> volumes, boolean hosts) {
        final Cursor<Record2<Long,Long>> cursor = create()
                .select(HOST.ID, STORAGE_POOL.ID)
                .from(HOST)
                .leftOuterJoin(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.HOST_ID.eq(HOST.ID)
                        .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull()))
                .join(STORAGE_POOL)
                    .on(STORAGE_POOL.ID.eq(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID))
                .where(
                    STORAGE_POOL.KIND.eq("sim")
                    .and(HOST.KIND.eq("sim")))
                .fetchLazy();

        return new AllocationCandidateIterator(cursor, volumes, hosts);
    }

    @Override
    public boolean isSimulatedVolume(long volumeId) {
        Volume volume = objectManager.loadResource(Volume.class, volumeId);
        Long instanceId = volume.getInstanceId();

        return instanceId == null ? false : isSimulatedInstance(instanceId);
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
