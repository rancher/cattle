package io.github.ibuildthecloud.agent.connection.ssh;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.SessionListener;

public class CloseListener implements SessionListener {

    SshAgentConnection connection;

    public CloseListener(SshAgentConnection connection) {
        super();
        this.connection = connection;
    }

    @Override
    public void sessionCreated(Session session) {
    }

    @Override
    public void sessionChanged(Session session) {
    }

    @Override
    public void sessionClosed(Session session) {
        connection.close();
    }

}
