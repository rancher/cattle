package io.cattle.platform.agent.connection.ssh.connection;

import java.io.IOException;
import java.util.concurrent.Executor;

import org.apache.sshd.common.Channel;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoSession;

public class EofClosingTcpipServerChannel extends TcpipServerChannel {

    Executor executor;

    public static class EofClosingTcpipServerChannelFactory implements NamedFactory<Channel> {

        Executor executor;

        public EofClosingTcpipServerChannelFactory(Executor executor) {
            super();
            this.executor = executor;
        }

        @Override
        public String getName() {
            return "forwarded-tcpip";
        }

        @Override
        public Channel create() {
            return new EofClosingTcpipServerChannel(Type.Forwarded, executor);
        }
    }

    public EofClosingTcpipServerChannel(Type type, Executor executor) {
        super(type);
        this.executor = executor;
    }

    @Override
    public void handleEof() throws IOException {
        super.handleEof();

        IoSession ioSession = getIoSession();
        if (ioSession != null) {
            ioSession.close(true);
        }
    }

    @Override
    public CloseFuture close(boolean immediately) {
        return super.close(immediately).addListener(new SshFutureListener<CloseFuture>() {
            @Override
            public void operationComplete(CloseFuture sshFuture) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        getConnector().dispose();
                    }
                });
            }
        });
    }

}
