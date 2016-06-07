package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.Map;

@Type(list = false)
public class VirtualMachineDisk {

    String name;
    String size;
    Integer readIops;
    Integer writeIops;
    String driver;
    boolean root;

    Map<String, String> opts;

    public Map<String, String> getOpts() {
        return opts;
    }

    public void setOpts(Map<String, String> opts) {
        this.opts = opts;
    }

    public String getName() {
        return name;
    }

    @Field(nullable=true, validChars="a-z0-9_.-", minLength=2)
    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    @Field(defaultValue="40g")
    public void setSize(String size) {
        this.size = size;
    }

    public Integer getReadIops() {
        return readIops;
    }

    public void setReadIops(Integer readIops) {
        this.readIops = readIops;
    }

    public Integer getWriteIops() {
        return writeIops;
    }

    public void setWriteIops(Integer writeIops) {
        this.writeIops = writeIops;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

}