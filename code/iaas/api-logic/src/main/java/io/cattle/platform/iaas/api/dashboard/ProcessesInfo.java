package io.cattle.platform.iaas.api.dashboard;

public class ProcessesInfo {
    private Long now;
    private Long slow;
    private Long recent;
    private Long recentThreshold;

    public ProcessesInfo(Long now, Long slow, Long recent, Long recentThreshold) {
        this.now = now;
        this.slow = slow;
        this.recent = recent;
        this.recentThreshold = recentThreshold;
    }

    public Long getRecentThreshold() {
        return recentThreshold;
    }

    public Long getNow() {
        return now;
    }

    public void setNow(Long now) {
        this.now = now;
    }

    public Long getSlow() {
        return slow;
    }

    public void setSlow(Long slow) {
        this.slow = slow;
    }

    public Long getRecent() {
        return recent;
    }

    public void setRecent(Long recent) {
        this.recent = recent;
    }

    public void setRecentThreshold(Long recentThreshold) {
        this.recentThreshold = recentThreshold;
    }
}
