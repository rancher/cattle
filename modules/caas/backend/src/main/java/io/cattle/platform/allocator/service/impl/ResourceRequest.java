package io.cattle.platform.allocator.service.impl;

/*
 *  Interface type for ResourceRequest
 *  Types:
 *  1. memoryReservation
 *  2. storageSize
 *  3. cpuReservation
 *  4. instanceReservation
 *  5. portReservation
 *  Any ResourceRequest should implement this interface and add their own data structure
 */
public interface ResourceRequest {
    String getResource();
    
    String getType();

}
