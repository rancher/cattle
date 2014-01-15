package io.github.ibuildthecloud.agent.connection.ssh;

import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.agent.server.connection.AgentConnectionFactory;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.eventing.EventService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.SftpClient;
import org.apache.sshd.client.SftpClient.Attributes;
import org.apache.sshd.client.SftpClient.Handle;
import org.apache.sshd.client.SftpClient.OpenMode;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.SshdSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;

public class SshAgentConnectionFactory implements AgentConnectionFactory {

    private static final DynamicLongProperty AUTH_TIMEOUT = ArchaiusUtil.getLong("ssh.auth.timeout.millis");
    private static final DynamicStringProperty BOOTSTRAP_FILE = ArchaiusUtil.getString("ssh.bootstrap.destination");
    private static final DynamicStringProperty BOOTSTRAP_SOURCE = ArchaiusUtil.getString("ssh.bootstrap.source");
    private static final DynamicStringProperty BOOTSTRAP_SOURCE_OVERRIDE = ArchaiusUtil.getString("ssh.bootstrap.source.override");

    private static Logger log = LoggerFactory.getLogger(SshAgentConnectionFactory.class);

    private static Pattern SSH_PATTERN = Pattern.compile("ssh://"
                                            + "([^:]+)(:([^@]+))?@"
                                            + "([^:]+)(:([0-9]+))?");
    private static String PROTOCOL = "ssh://";

    SecureRandom random = new SecureRandom();
    SshClient client = null;
    EventService eventService;
    Map<Long,SshAgentConnection> connections = new ConcurrentHashMap<Long, SshAgentConnection>();

    @Override
    public AgentConnection createConnection(Agent agent) throws IOException {
        String uri = agent.getUri();
        if ( uri == null || ! uri.startsWith(PROTOCOL) ) {
            return null;
        }

        SshConnectionOptions opts = parseOpts(uri);
        if ( opts == null ) {
            return null;
        }

        try {
            return createConnection(agent, opts);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    protected SshConnectionOptions parseOpts(String uri) {
        Matcher m = SSH_PATTERN.matcher(uri);
        if ( ! m.matches() ) {
            log.error("{} does not match ssh option, must be in the format "
                    + "ssh://user:password@host:port where password and port are optional");
            return null;
        }

        String username = m.group(1);
        String password = m.group(3);
        String host = m.group(4);
        String port = m.group(6);

        return new SshConnectionOptions(host, port, username, password);
    }

    protected AgentConnection createConnection(Agent agent, SshConnectionOptions opts) throws IOException, InterruptedException {
        SshClient client = getClient();
        ClientSession session = null;
        boolean success = false;
        try {
            session = connect(client, opts);

            String script = copyBootStrap(session);
            int port = setForwarding(session);
            log.info("Allocated random port [{}] on [{}]", port, opts.getHost());
            ChannelExec exec = callBootStrap(session, script);

            success = true;
            SshAgentConnection sshAgent = new SshAgentConnection(agent.getId(), agent.getUri(), eventService, this, session, exec, port);
            session.addListener(new CloseListener(sshAgent));

            return sshAgent;
        } finally {
            if ( ! success && session != null ) {
                session.close(true);
            }
        }
    }

    protected void close(SshAgentConnection agentConnection) {
        agentConnection.getSession().close(true);
    }

    protected ChannelExec callBootStrap(ClientSession session, String script) throws IOException {
        ChannelExec exec = session.createExecChannel(script);
        return exec;
    }

    protected String copyBootStrap(ClientSession session) throws IOException {
        String dest = BOOTSTRAP_FILE.get() + random.nextLong();
        SftpClient sftpClient = session.createSftpClient();
        try {
            Handle handle = sftpClient.open(dest, EnumSet.of(OpenMode.Read, OpenMode.Write, OpenMode.Create, OpenMode.Exclusive));
            Attributes attr = new Attributes().perms(0700);
            sftpClient.setStat(handle, attr);
            byte[] content = getBootstrapSource(BOOTSTRAP_SOURCE_OVERRIDE.get(), BOOTSTRAP_SOURCE.get());
            sftpClient.write(handle, 0, content, 0, content.length);
            sftpClient.close(handle);

            return dest;
        } finally {
            if ( sftpClient != null ) {
                sftpClient.close();
            }
        }
    }

    protected int setForwarding(ClientSession session) throws IOException {
        SshdSocketAddress address = session.startRemotePortForwarding(new SshdSocketAddress("localhost", 0), new SshdSocketAddress("localhost", 8080));
        return address.getPort();
    }

    protected ClientSession connect(SshClient client, SshConnectionOptions opts) throws IOException, InterruptedException {
        ConnectFuture connect = client.connect(opts.getHost(), opts.getPort());
        connect.await(AUTH_TIMEOUT.get());
        ClientSession session = connect.getSession();
        if ( session == null ) {
            throw new IOException("Failed to create session to [" + opts.getHost() + "]");
        }

        session.authPassword(opts.getUsername(), opts.getPassword());

        int ret = session.waitFor(ClientSession.CLOSED | ClientSession.AUTHED | ClientSession.WAIT_AUTH, AUTH_TIMEOUT.get());

        if ((ret & ClientSession.AUTHED) != ClientSession.AUTHED) {
            throw new IOException("Failed to authenticate with [" + opts.getHost() + "]");
        }

        return session;
    }

    protected synchronized SshClient getClient() {
        if ( client != null ) {
            return client;
        }

        SshClient client = SshClient.setUpDefaultClient();
        client.setTcpipForwarderFactory(new DefaultTcpipForwarderFactory());
        client.setTcpipForwardingFilter(new ReverseConnectAllowFilter());

        client.start();

        return this.client = client;
    }

    protected byte[] getBootstrapSource(String... sources) throws IOException {
        ClassLoader cl = getClass().getClassLoader();
        for ( String source : sources ) {
            InputStream is = cl.getResourceAsStream(source);
            try {
                if ( is != null ) {
                    return IOUtils.toByteArray(is);
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        throw new FileNotFoundException("Failed to find [" + Arrays.toString(sources) + "]");
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public EventService getEventService() {
        return eventService;
    }
}
