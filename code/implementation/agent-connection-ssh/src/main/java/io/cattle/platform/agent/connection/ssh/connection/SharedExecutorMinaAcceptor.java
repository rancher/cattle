package io.cattle.platform.agent.connection.ssh.connection;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executor;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.mina.MinaAcceptor;

public class SharedExecutorMinaAcceptor extends MinaAcceptor {

    Executor executor;
    IoProcessor<NioSession> ioProcessor;

    public SharedExecutorMinaAcceptor(Executor executor, IoProcessor<NioSession> ioProcessor, FactoryManager manager, IoHandler handler) {
        super(manager, handler);
        this.executor = executor;
        this.ioProcessor = ioProcessor;
    }

    @Override
    protected IoAcceptor createAcceptor() {
        NioSocketAcceptor acceptor = new NioSocketAcceptor(executor, ioProcessor);
        acceptor.setCloseOnDeactivation(false);
        acceptor.setReuseAddress(reuseAddress);
        acceptor.setBacklog(backlog);

        // MINA itself forces our socket receive buffer to 1024 bytes
        // by default, despite what the operating system defaults to.
        // This limits us to about 3 MB/s incoming data transfer. By
        // forcing back to the operating system default we can get a
        // decent transfer rate again.
        //
        final Socket s = new Socket();
        try {
            try {
                acceptor.getSessionConfig().setReceiveBufferSize(s.getReceiveBufferSize());
            } finally {
                s.close();
            }
        } catch (IOException e) {
            log.warn("cannot adjust SO_RCVBUF back to system default", e);
        }
        if (sessionConfig != null) {
            acceptor.getSessionConfig().setAll(sessionConfig);
        }
        return acceptor;
    }

}
