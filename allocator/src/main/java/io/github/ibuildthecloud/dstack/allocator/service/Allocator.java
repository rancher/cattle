package io.github.ibuildthecloud.dstack.allocator.service;


public interface Allocator {

    public boolean allocate(AllocationRequest request);

    public boolean deallocate(AllocationRequest request);

}