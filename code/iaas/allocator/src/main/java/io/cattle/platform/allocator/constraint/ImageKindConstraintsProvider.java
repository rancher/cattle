package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;

import java.util.List;

import javax.inject.Inject;

public class ImageKindConstraintsProvider implements AllocationConstraintsProvider {

    String kind;
    AllocatorDao allocatorDao;
    boolean exclusive = true;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        Long instanceId = attempt.getInstanceId();
        boolean add = false;

        if ( instanceId == null ) {
            for ( long volumeId : attempt.getVolumeIds() ) {
                add = allocatorDao.isVolumeInstanceImageKind(volumeId, kind);
                if ( add ) {
                    break;
                }
            }

            if ( add ) {
                constraints.add(new StoragePoolKindConstraint(kind));
            } else if ( exclusive ) {
                constraints.add(new NegativeStoragePoolKindConstraint(kind));
            }
        } else {
            if ( allocatorDao.isInstanceImageKind(instanceId, kind) ) {
                constraints.add(new HostKindConstraint(kind));
            } else if ( exclusive ) {
                constraints.add(new NegativeHostKindConstraint(kind));
            }
        }
    }

    public String getKind() {
        return kind;
    }

    @Inject
    public void setKind(String kind) {
        this.kind = kind;
    }

    public AllocatorDao getAllocatorDao() {
        return allocatorDao;
    }

    @Inject
    public void setAllocatorDao(AllocatorDao allocatorDao) {
        this.allocatorDao = allocatorDao;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

}
