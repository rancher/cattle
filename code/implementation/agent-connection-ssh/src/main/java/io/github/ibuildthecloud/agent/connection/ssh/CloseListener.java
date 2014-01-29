package io.github.ibuildthecloud.agent.connection.ssh;

import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SessionListener;
import org.apache.sshd.common.future.SshFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloseListener implements SessionListener, SshFutureListener<OpenFuture> {

    private static final Logger log = LoggerFactory.getLogger(CloseListener.class);
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

    @Override
    public void operationComplete(OpenFuture future) {
        if ( ! future.isOpened() ) {
            log.error("Failed to execute bootstrap command for agent [{}]", connection.getAgentId(), future.getException());
            connection.close();
        }
    }

}
