package io.github.ibuildthecloud.dstack.simple.allocator.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.HostTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.ImageStoragePoolMapTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.ImageTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.InstanceTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.StoragePoolHostMapTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.StoragePoolTable.*;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;
import io.github.ibuildthecloud.dstack.core.constants.CommonStatesConstants;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.simple.allocator.dao.SimpleAllocatorDao;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.jooq.Cursor;
import org.jooq.Record2;

public class SimpleAllocatorDaoImpl extends AbstractJooqDao implements SimpleAllocatorDao {

    ObjectManager objectManager;

    @Override
    public boolean isInstance(long instanceId, String kind) {
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
                    .and(STORAGE_POOL.KIND.eq(kind)))
                .fetch().size() > 0;
    }

    @Override
    public Iterator<AllocationCandidate> iteratorPools(List<Long> volumes, String kind) {
        return iteratorHosts(volumes, kind, false);
    }

    @Override
    public Iterator<AllocationCandidate> iteratorHosts(List<Long> volumes, String kind) {
        return iteratorHosts(volumes, kind, true);
    }

    protected Iterator<AllocationCandidate> iteratorHosts(List<Long> volumes, String kind, boolean hosts) {
        final Cursor<Record2<Long,Long>> cursor = create()
                .select(HOST.ID, STORAGE_POOL.ID)
                .from(HOST)
                .leftOuterJoin(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.HOST_ID.eq(HOST.ID)
                        .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull()))
                .join(STORAGE_POOL)
                    .on(STORAGE_POOL.ID.eq(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID))
                .where(
                    HOST.STATE.eq(CommonStatesConstants.ACTIVE)
                    .and(STORAGE_POOL.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(STORAGE_POOL.KIND.eq(kind))
                    .and(HOST.KIND.eq(kind)))
                .fetchLazy();

        return new AllocationCandidateIterator(cursor, volumes, hosts);
    }

    @Override
    public boolean isVolume(long volumeId, String kind) {
        Volume volume = objectManager.loadResource(Volume.class, volumeId);
        Long instanceId = volume.getInstanceId();

        return instanceId == null ? false : isInstance(instanceId, kind);
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
