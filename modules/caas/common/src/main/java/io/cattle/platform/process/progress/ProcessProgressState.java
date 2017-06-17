package io.cattle.platform.process.progress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

class ProcessProgressState {

    boolean inCorrectCheckPoint = false;
    String currentCheckpoint;
    String subMessage = null;
    List<String> checkPoints = new ArrayList<>();

    public String getMessage() {
        String prefix = checkPoints.size() > 0 ? checkPoints.get(checkPoints.size() - 1) : null;

        if (prefix == null) {
            return null;
        }

        if (StringUtils.isBlank(subMessage)) {
            return prefix;
        } else {
            return String.format("%s : %s", prefix, subMessage);
        }
    }

    public boolean checkPoint(String name) {
        currentCheckpoint = name;

        if (!checkPoints.contains(name)) {
            checkPoints.add(name);
            subMessage = null;
            inCorrectCheckPoint = true;
            return true;
        } else if (name != null && checkPoints.size() > 0 && name.equals(checkPoints.get(checkPoints.size() - 1))) {
            inCorrectCheckPoint = true;
        }

        return false;
    }

    public boolean setMessage(String message) {
        if (!Objects.equals(this.subMessage, message)) {
            this.subMessage = message;
            return true;
        }

        return false;
    }

    public List<String> getCheckPoints() {
        return checkPoints;
    }

    public void setCheckPoints(List<String> checkPoints) {
        this.checkPoints = checkPoints;
    }

    public String getSubMessage() {
        return subMessage;
    }

    public void setSubMessage(String subMessage) {
        this.subMessage = subMessage;
    }

}