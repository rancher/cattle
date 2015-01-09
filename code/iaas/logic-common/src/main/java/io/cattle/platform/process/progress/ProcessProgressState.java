package io.cattle.platform.process.progress;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class ProcessProgressState {

    boolean inCorrectCheckPoint = false;
    int[] checkpointWeights;
    String currentCheckpoint;
    String subMessage = null;
    List<String> checkPoints = new ArrayList<String>();
    Integer progress = null;
    Integer intermediateProgress = null;

    public ProcessProgressState() {
        super();
    }

    public ProcessProgressState(int[] weights) {
        super();
        this.checkpointWeights = weights;
    }

    public void setCheckpointWeights(int... weights) {
        this.checkpointWeights = weights;
    }

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
            intermediateProgress = null;
            subMessage = null;
            inCorrectCheckPoint = true;

            calculatePercentage();
            return true;
        } else if (name != null && checkPoints.size() > 0 && name.equals(checkPoints.get(checkPoints.size() - 1))) {
            inCorrectCheckPoint = true;
        }

        return false;
    }

    public boolean setMessage(String message) {
        if (!ObjectUtils.equals(this.subMessage, message)) {
            this.subMessage = message;
            return true;
        }

        return false;
    }

    public boolean setIntermediateProgress(Integer progress) {
        this.intermediateProgress = progress;

        if (inCorrectCheckPoint) {
            calculatePercentage();
            return true;
        }

        return false;
    }

    protected void calculatePercentage() {
        if (checkpointWeights == null || checkpointWeights.length == 0) {
            progress = null;
            return;
        }

        int percentage = 0;
        int last = 0;

        for (int i = 0; i < checkPoints.size(); i++) {
            percentage += last;

            if (checkpointWeights.length > i) {
                last = checkpointWeights[i];
            } else {
                last = 0;
            }
        }

        if (intermediateProgress != null && intermediateProgress > 0) {
            percentage += ((last * Math.min(intermediateProgress, 100)) / 100);
        }

        if (percentage > 100) {
            progress = null;
        } else {
            progress = percentage;
        }
    }

    public String getCurrentCheckpoint() {
        return currentCheckpoint;
    }

    public List<String> getCheckPoints() {
        return checkPoints;
    }

    public void setCheckPoints(List<String> checkPoints) {
        this.checkPoints = checkPoints;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public int[] getCheckpointWeights() {
        return checkpointWeights;
    }

    public Integer getIntermediateProgress() {
        return intermediateProgress;
    }

    public String getSubMessage() {
        return subMessage;
    }

    public void setSubMessage(String subMessage) {
        this.subMessage = subMessage;
    }

}