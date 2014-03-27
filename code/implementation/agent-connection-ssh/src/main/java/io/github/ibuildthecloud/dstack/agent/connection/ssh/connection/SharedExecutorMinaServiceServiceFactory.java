package io.github.ibuildthecloud.dstack.agent.connection.ssh.connection;

import java.util.concurrent.Executor;

import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.io.IoAcceptor;
import org.apache.sshd.common.io.IoConnector;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.IoServiceFactory;

public class SharedExecutorMinaServiceServiceFactory implements IoServiceFactory {

    Executor executor;
    IoProcessor<NioSession> ioProcessor;

    public SharedExecutorMinaServiceServiceFactory(Executor executor) {
        super();
        this.executor = executor;
        this.ioProcessor = new SimpleIoProcessorPool<NioSession>(NioProcessor.class, executor);

    }

    @Override
    public IoConnector createConnector(FactoryManager manager, IoHandler handler) {
        return new SharedExecutorMinaConnector(executor, ioProcessor, manager, handler);
    }

    @Override
    public IoAcceptor createAcceptor(FactoryManager manager, IoHandler handler) {
        return new SharedExecutorMinaAcceptor(executor, ioProcessor, manager, handler);
    }

}
