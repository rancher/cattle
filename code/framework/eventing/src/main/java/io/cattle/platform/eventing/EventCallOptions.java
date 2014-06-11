package io.cattle.platform.eventing;

public class EventCallOptions {

    Integer retry;
    Long timeoutMillis;
    EventProgress progress;
    RetryCallback retryCallback;

    public EventCallOptions() {
    }

    public EventCallOptions(EventCallOptions other) {
        super();
        this.retry = other.getRetry();
        this.timeoutMillis = other.getTimeoutMillis();
        this.progress = other.getProgress();
        this.retryCallback = other.getRetryCallback();
    }

    public EventCallOptions(Integer retry, Long timeoutMillis) {
        super();
        this.retry = retry;
        this.timeoutMillis = timeoutMillis;
    }

    public Integer getRetry() {
        return retry;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public EventCallOptions withRetry(Integer retry) {
        this.retry = retry;
        return this;
    }

    public Long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(Long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public EventCallOptions withTimeoutMillis(Long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public EventProgress getProgress() {
        return progress;
    }

    public void setProgress(EventProgress progress) {
        this.progress = progress;
    }

    public EventCallOptions withProgress(EventProgress progress) {
        this.progress = progress;
        return this;
    }

    public RetryCallback getRetryCallback() {
        return retryCallback;
    }

    public void setRetryCallback(RetryCallback retryCallback) {
        this.retryCallback = retryCallback;
    }

    public EventCallOptions withRetryCallback(RetryCallback retryCallback) {
        this.retryCallback = retryCallback;
        return this;
    }

}
