package io.cattle.platform.agent.connection.ssh;

import io.cattle.platform.agent.connection.event.AgentEventingConnection;
import io.cattle.platform.agent.server.connection.AgentConnection;
import io.cattle.platform.eventing.EventService;

import org.apache.sshd.ClientSession;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.common.Session.State;

public class SshAgentConnection extends AgentEventingConnection implements AgentConnection {

    ClientSession session;
    ChannelExec exec;
    SshAgentConnectionFactory factory;
    int callbackPort;

    public SshAgentConnection(long agentId, String uri, EventService eventService, SshAgentConnectionFactory factory,
            ClientSession session, ChannelExec exec, int callbackPort) {
        super(agentId, uri, eventService);

        this.factory = factory;
        this.session = session;
        this.exec = exec;
        this.callbackPort = callbackPort;
    }

    @Override
    public void close() {
        super.close();
        factory.close(this);
    }


    @Override
    public boolean isOpen() {
        if ( ! super.isOpen() ) {
            return false;
        }

        return session.getState() == State.Running;
    }

    public ClientSession getSession() {
        return session;
    }

    public void setSession(ClientSession session) {
        this.session = session;
    }

    public ChannelExec getExec() {
        return exec;
    }

    public void setExec(ChannelExec exec) {
        this.exec = exec;
    }

    public int getCallbackPort() {
        return callbackPort;
    }

}
