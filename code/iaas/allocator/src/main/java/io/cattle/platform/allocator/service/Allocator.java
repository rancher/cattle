package io.cattle.platform.allocator.service;

public interface Allocator {

    public void allocate(AllocationRequest request);

    public void deallocate(AllocationRequest request);

}