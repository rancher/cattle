package io.cattle.platform.agent.connection.ssh.connection;

import java.util.concurrent.Executor;

import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.mina.MinaConnector;

public class SharedExecutorMinaConnector extends MinaConnector {

    Executor executor;
    IoProcessor<NioSession> ioProcessor;

    public SharedExecutorMinaConnector(Executor executor, IoProcessor<NioSession> ioProcessor, FactoryManager manager, IoHandler handler) {
        super(manager, handler);
        this.executor = executor;
        this.ioProcessor = ioProcessor;
    }

    @Override
    protected IoConnector createConnector() {
        NioSocketConnector connector = new NioSocketConnector(executor, ioProcessor);
        if (sessionConfig != null) {
            connector.getSessionConfig().setAll(sessionConfig);
        }
        return connector;
    }

}
