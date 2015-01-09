package io.cattle.platform.allocator.service;

public interface Allocator {

    public boolean allocate(AllocationRequest request);

    public boolean deallocate(AllocationRequest request);

}