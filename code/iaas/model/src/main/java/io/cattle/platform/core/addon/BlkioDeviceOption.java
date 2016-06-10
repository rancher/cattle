package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class BlkioDeviceOption {

    Integer readIops;
    Integer writeIops;
    Integer readBps;
    Integer writeBps;
    Integer weight;

    public Integer getReadIops() {
        return readIops;
    }

    public void setReadIops(Integer readIOps) {
        this.readIops = readIOps;
    }

    public Integer getWriteIops() {
        return writeIops;
    }

    public void setWriteIops(Integer writeIOps) {
        this.writeIops = writeIOps;
    }

    public Integer getReadBps() {
        return readBps;
    }

    public void setReadBps(Integer readBps) {
        this.readBps = readBps;
    }

    public Integer getWriteBps() {
        return writeBps;
    }

    public void setWriteBps(Integer writeBps) {
        this.writeBps = writeBps;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

}
