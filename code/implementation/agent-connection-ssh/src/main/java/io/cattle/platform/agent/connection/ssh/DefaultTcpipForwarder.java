/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.cattle.platform.agent.connection.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sshd.ClientChannel;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.common.TcpipForwarder;
import org.apache.sshd.common.forward.TcpipClientChannel;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoAcceptor;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.common.util.Readable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Add javadoc
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class DefaultTcpipForwarder implements TcpipForwarder, IoHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTcpipForwarder.class);

    private final Session session;
    private final Map<Integer, SshdSocketAddress> localToRemote = new HashMap<Integer, SshdSocketAddress>();
    private final Map<Integer, SshdSocketAddress> remoteToLocal = new HashMap<Integer, SshdSocketAddress>();
    private final Set<SshdSocketAddress> localForwards = new HashSet<SshdSocketAddress>();
    protected IoAcceptor acceptor;

    public DefaultTcpipForwarder(Session session) {
        this.session = session;
    }

    //
    // TcpIpForwarder implementation
    //

    @Override
    public synchronized SshdSocketAddress startLocalPortForwarding(SshdSocketAddress local, SshdSocketAddress remote) throws IOException {
        if (local == null) {
            throw new IllegalArgumentException("Local address is null");
        }
        if (remote == null) {
            throw new IllegalArgumentException("Remote address is null");
        }
        if (local.getPort() < 0) {
            throw new IllegalArgumentException("Invalid local port: " + local.getPort());
        }
        SshdSocketAddress bound = doBind(local);
        localToRemote.put(bound.getPort(), remote);

        return bound;
    }

    @Override
    public synchronized void stopLocalPortForwarding(SshdSocketAddress local) throws IOException {
        if (localToRemote.remove(local.getPort()) != null && acceptor != null) {
            acceptor.unbind(local.toInetSocketAddress());
            if (acceptor.getBoundAddresses().isEmpty()) {
                close();
            }
        }
    }

    @Override
    public synchronized SshdSocketAddress startRemotePortForwarding(SshdSocketAddress remote, SshdSocketAddress local) throws IOException {
        Buffer buffer = session.createBuffer(SshConstants.Message.SSH_MSG_GLOBAL_REQUEST, 0);
        buffer.putString("tcpip-forward");
        buffer.putBoolean(true);
        buffer.putString(remote.getHostName());
        buffer.putInt(remote.getPort());
        Buffer result = session.request(buffer);
        if (result == null) {
            throw new SshException("Tcpip forwarding request denied by server");
        }
        int port = remote.getPort() == 0 ? result.getInt() : remote.getPort();
        // TODO: Is it really safe to only store the local address after the
        // request ?
        remoteToLocal.put(port, local);

        if (remote.getPort() == 0) {
            /*
             * TODO: This will create a leak if stopRemotePortForwarding is
             * called because it won't be removed. So this hack is incomplete,
             * but works if you never change forwarding during the session.
             */
            remoteToLocal.put(0, local);
        }

        return new SshdSocketAddress(remote.getHostName(), port);
    }

    @Override
    public synchronized void stopRemotePortForwarding(SshdSocketAddress remote) throws IOException {
        if (remoteToLocal.remove(remote.getPort()) != null) {
            Buffer buffer = session.createBuffer(SshConstants.Message.SSH_MSG_GLOBAL_REQUEST, 0);
            buffer.putString("cancel-tcpip-forward");
            buffer.putBoolean(false);
            buffer.putString(remote.getHostName());
            buffer.putInt(remote.getPort());
            session.writePacket(buffer);
        }
    }

    @Override
    public synchronized SshdSocketAddress getForwardedPort(int remotePort) {
        return remoteToLocal.get(remotePort);
    }

    @Override
    public synchronized SshdSocketAddress localPortForwardingRequested(SshdSocketAddress local) throws IOException {
        if (local == null) {
            throw new IllegalArgumentException("Local address is null");
        }
        if (local.getPort() < 0) {
            throw new IllegalArgumentException("Invalid local port: " + local.getPort());
        }
        final ForwardingFilter filter = session.getFactoryManager().getTcpipForwardingFilter();
        if (filter == null || !filter.canListen(local, session)) {
            throw new IOException("Rejected address: " + local);
        }
        SshdSocketAddress bound = doBind(local);
        localForwards.add(bound);
        return bound;
    }

    @Override
    public synchronized void localPortForwardingCancelled(SshdSocketAddress local) throws IOException {
        if (localForwards.remove(local) && acceptor != null) {
            acceptor.unbind(local.toInetSocketAddress());
            if (acceptor.getBoundAddresses().isEmpty()) {
                close();
            }
        }
    }

    public synchronized void initialize() {
        if (this.acceptor == null) {
            this.acceptor = session.getFactoryManager().getIoServiceFactory().createAcceptor(session.getFactoryManager(), this);
        }
    }

    @Override
    public synchronized void close() {
        if (acceptor != null) {
            acceptor.dispose();
            acceptor = null;
        }
    }

    //
    // IoHandler implementation
    //

    @Override
    public void sessionCreated(final IoSession session) throws Exception {
        final TcpipClientChannel channel;
        int localPort = ((InetSocketAddress) session.getLocalAddress()).getPort();
        if (localToRemote.containsKey(localPort)) {
            SshdSocketAddress remote = localToRemote.get(localPort);
            channel = new TcpipClientChannel(TcpipClientChannel.Type.Direct, session, remote);
        } else {
            channel = new TcpipClientChannel(TcpipClientChannel.Type.Forwarded, session, null);
        }
        session.setAttribute(TcpipClientChannel.class, channel);
        this.session.registerChannel(channel);
        channel.open().addListener(new SshFutureListener<OpenFuture>() {
            @Override
            public void operationComplete(OpenFuture future) {
                Throwable t = future.getException();
                if (t != null) {
                    DefaultTcpipForwarder.this.session.unregisterChannel(channel);
                    channel.close(false);
                }
            }
        });
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        TcpipClientChannel channel = (TcpipClientChannel) session.getAttribute(TcpipClientChannel.class);
        if (channel != null) {
            LOGGER.debug("Session closed, will now close the channel");
            channel.close(false);
        }
    }

    @Override
    public void messageReceived(IoSession session, Readable message) throws Exception {
        TcpipClientChannel channel = (TcpipClientChannel) session.getAttribute(TcpipClientChannel.class);
        Buffer buffer = new Buffer();
        buffer.putBuffer(message);
        channel.waitFor(ClientChannel.OPENED | ClientChannel.CLOSED, Long.MAX_VALUE);
        channel.getOut().write(buffer.array(), buffer.rpos(), buffer.available());
        channel.getOut().flush();
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
        session.close(false);
    }

    //
    // Private methods
    //

    private SshdSocketAddress doBind(SshdSocketAddress address) throws IOException {
        initialize();
        Set<SocketAddress> before = acceptor.getBoundAddresses();
        try {
            acceptor.bind(address.toInetSocketAddress());
            Set<SocketAddress> after = acceptor.getBoundAddresses();
            after.removeAll(before);
            if (after.isEmpty()) {
                throw new IOException("Error binding to " + address + ": no local addresses bound");
            }
            if (after.size() > 1) {
                throw new IOException("Multiple local addresses have been bound for " + address);
            }
            InetSocketAddress result = (InetSocketAddress) after.iterator().next();
            return new SshdSocketAddress(address.getHostName(), result.getPort());
        } catch (IOException bindErr) {
            if (acceptor.getBoundAddresses().isEmpty()) {
                close();
            }
            throw bindErr;
        }
    }

}
