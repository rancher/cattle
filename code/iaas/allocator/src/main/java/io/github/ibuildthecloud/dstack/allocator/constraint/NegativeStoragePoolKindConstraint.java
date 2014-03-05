package io.github.ibuildthecloud.dstack.allocator.constraint;

import io.github.ibuildthecloud.dstack.allocator.service.AllocationAttempt;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationCandidate;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;

import java.util.Map;
import java.util.Set;

public class NegativeStoragePoolKindConstraint implements Constraint {

    String kind;

    public NegativeStoragePoolKindConstraint(String kind) {
        super();
        this.kind = kind;
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        for ( Map.Entry<Long,Set<Long>> entry : candidate.getPools().entrySet() ) {
            for ( Long id : entry.getValue() ) {
                StoragePool pool = candidate.loadResource(StoragePool.class, id);

                if ( kind.equals(pool.getKind()) ) {
                    return false;
                }
            }
        }

        return true;
    }


    @Override
    public String toString() {
        return String.format("storagePool.kind must not be %s", kind);
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

}
