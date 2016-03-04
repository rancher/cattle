package io.cattle.platform.iaas.api.dashboard;

import java.util.List;

public class Bucket {
    private float start;
    private float end;
    private List<String> ids;

    public Bucket(float start, float end, List<String> ids) {
        this.start = start;
        this.end = end;
        this.ids = ids;
    }

    public float getStart() {
        return start;
    }

    public void setStart(float start) {
        this.start = start;
    }

    public float getEnd() {
        return end;
    }

    public void setEnd(float end) {
        this.end = end;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public boolean addValue(double value, String id) {
        if (start <= value && value < end) {
            ids.add(id);
            return true;
        }
        return false;
    }
}
