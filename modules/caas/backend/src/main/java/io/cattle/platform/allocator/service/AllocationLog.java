package io.cattle.platform.allocator.service;

import java.util.ArrayList;
import java.util.List;

public class AllocationLog {

    List<AllocationAttempt> attempts = new ArrayList<AllocationAttempt>();

    public List<AllocationAttempt> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<AllocationAttempt> attempts) {
        this.attempts = attempts;
    }

}
