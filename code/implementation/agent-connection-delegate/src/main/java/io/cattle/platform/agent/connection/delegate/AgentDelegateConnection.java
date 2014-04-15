package io.cattle.platform.agent.connection.delegate;

import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.server.connection.AgentConnection;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.delegate.DelegateEvent;
import io.cattle.platform.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class AgentDelegateConnection implements AgentConnection {

    String uri;
    long agentId;
    boolean open = true;
    RemoteAgent remoteAgent;
    Map<String,Object> instanceData;
    JsonMapper jsonMapper;

    public AgentDelegateConnection(RemoteAgent remoteAgent, long agentId, String uri, Map<String,Object> instanceData,
            JsonMapper jsonMapper) {
        super();
        this.remoteAgent = remoteAgent;
        this.agentId = agentId;
        this.uri = uri;
        this.jsonMapper = jsonMapper;
        this.instanceData = instanceData;
    }

    @Override
    public long getAgentId() {
        return agentId;
    }

    @Override
    public ListenableFuture<Event> execute(final Event event) {
        if ( ! open ) {
            return AsyncUtils.error(new IOException("Agent connection is closed"));
        }

        DelegateEvent delegateEvent = new DelegateEvent(instanceData, event);

        EventCallOptions options = new EventCallOptions(null, event.getTimeoutMillis());
        return Futures.transform(remoteAgent.call(delegateEvent, options), new AsyncFunction<Event, Event>() {
            @Override
            public ListenableFuture<Event> apply(Event input) throws Exception {
                if ( input == null ) {
                    return AsyncUtils.done(null);
                }

                Object data = input.getData();

                if ( data == null ) {
                    throw new IllegalStateException("No response for delegated input event [" + event.getName() + "] [" + event.getId() + "]");
                }

                return AsyncUtils.done((Event)jsonMapper.convertValue(data, EventVO.class));
            }
        });
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

}