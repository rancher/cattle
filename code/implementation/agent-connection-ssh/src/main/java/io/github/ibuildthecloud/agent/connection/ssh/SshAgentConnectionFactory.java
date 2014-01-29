package io.github.ibuildthecloud.agent.connection.ssh;

import io.github.ibuildthecloud.agent.connection.ssh.dao.SshAgentDao;
import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.agent.server.connection.AgentConnectionFactory;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.iaas.config.ScopedConfig;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.SftpClient;
import org.apache.sshd.client.SftpClient.Attributes;
import org.apache.sshd.client.SftpClient.Handle;
import org.apache.sshd.client.SftpClient.OpenMode;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.common.SshdSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;

public class SshAgentConnectionFactory implements AgentConnectionFactory {

    public static final String ACCESS_KEY = "DSTACK_ACCESS_KEY";
    public static final String SECRET_KEY = "DSTACK_SECRET_KEY";
    public static final String DEFAULT = "default";
    public static final String URL_CONFIG = "urlConfig";

    private static final DynamicStringProperty ADDITIONAL_ENV = ArchaiusUtil.getString("ssh.agent.env");
    private static final DynamicLongProperty AUTH_TIMEOUT = ArchaiusUtil.getLong("ssh.auth.timeout.millis");
    private static final DynamicStringProperty BOOTSTRAP_FILE = ArchaiusUtil.getString("ssh.bootstrap.destination");
    private static final DynamicStringProperty BOOTSTRAP_SOURCE = ArchaiusUtil.getString("ssh.bootstrap.source");
    private static final DynamicStringProperty BOOTSTRAP_SOURCE_OVERRIDE = ArchaiusUtil.getString("ssh.bootstrap.source.override");
    private static final DynamicStringProperty LOCALHOST_URL_FORMAT = ArchaiusUtil.getString("ssh.localhost.url.format");

    private static final Logger log = LoggerFactory.getLogger(SshAgentConnectionFactory.class);

    private static final Pattern SSH_PATTERN = Pattern.compile("ssh://"
                                            + "([^:]+)(:([^@]+))?@"
                                            + "([^:]+)(:([0-9]+))?");
    private static final String PROTOCOL = "ssh://";
    private static final Map<String,String> URLS = new LinkedHashMap<String, String>();
    static {
        URLS.put(ScopedConfig.API_URL, "DSTACK_URL");
        URLS.put(ScopedConfig.CONFIG_URL, "DSTACK_CONFIG_URL");
        URLS.put(ScopedConfig.STORAGE_URL, "DSTACK_STORAGE_URL");
    }

    SshAgentDao agentDao;
    SecureRandom random = new SecureRandom();
    SshClient client = null;
    EventService eventService;
    ObjectManager objectManager;
    Set<SshAgentConnection> connections = new ConcurrentHashSet<SshAgentConnection>();
    KeyPairProvider keyPairProvider;
    ScopedConfig scopedConfig;

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
            EofAwareChannelExec exec = callBootStrap(agent, session, script);

            final SshAgentConnection sshAgent = new SshAgentConnection(agent.getId(), agent.getUri(), eventService, this, session, exec, port);
            success = true;
            connections.add(sshAgent);

            exec.onEof(new Runnable() {
                @Override
                public void run() {
                    sshAgent.close();
                }
            });
            CloseListener listener = new CloseListener(sshAgent);
            session.addListener(listener);
            exec.open().addListener(listener);

            success = writeAuth(sshAgent);
            if ( ! success ) {
                sshAgent.close();
                throw new IOException("Failed to write context info for agent [" + sshAgent.getAgentId() + "]");
            }

            return sshAgent;
        } finally {
            if ( ! success && session != null ) {
                session.close(true);
            }
        }
    }

    protected void close(SshAgentConnection agentConnection) {
        log.info("Closing SSH agent connect for [{}]", agentConnection.getAgentId());
        agentConnection.getSession().close(true);
        connections.remove(agentConnection);
    }

    protected EofAwareChannelExec callBootStrap(Agent agent, ClientSession session, String script) throws IOException {
        EofAwareChannelExec exec = new EofAwareChannelExec(script);
        try {
            session.registerChannel(exec);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register exec channel", e);
        }
        exec.setOut(new LogOutputStream(log, "Agent [" + agent.getId() + "] Output [{}]", agent.getId().toString(), false));
        exec.setErr(new LogOutputStream(log, "Agent [" + agent.getId() + "] Error  [{}]", agent.getId().toString(), true));
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

        KeyPair kp = keyPairProvider.loadKey(KeyPairProvider.SSH_RSA);
        session.authPublicKey(opts.getUsername(), kp);
        if ( ! authSuccess(session) ) {
            session.authPassword(opts.getUsername(), opts.getPassword());
        }

        if ( ! authSuccess(session) ) {
            throw new IOException("Failed to authenticate with [" + opts.getHost() + "]");
        }

        return session;
    }

    protected boolean authSuccess(ClientSession session) {
        int ret = session.waitFor(ClientSession.CLOSED | ClientSession.AUTHED | ClientSession.WAIT_AUTH, AUTH_TIMEOUT.get());

        if ((ret & ClientSession.AUTHED) != ClientSession.AUTHED) {
            return false;
        }

        return true;
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

    protected boolean writeAuth(SshAgentConnection connection) throws IOException {
        if ( ! connection.isOpen() ) {
            return false;
        }

        Agent agent = objectManager.loadResource(Agent.class, connection.getAgentId());
        if ( agent == null ) {
            return false;
        }

        String[] keys = agentDao.getLastestActiveApiKeys(agent);
        if ( keys == null ) {
            log.error("Failed to find active API keys for agent [{}]", agent.getId());
        }

        Map<String,String> data = new LinkedHashMap<String, String>();
        data.put(ACCESS_KEY, keys[0]);
        data.put(SECRET_KEY, keys[1]);

        Map<String,Object> config = CollectionUtils.castMap(CollectionUtils.castMap(agent.getData()).get(URL_CONFIG));

        for ( Map.Entry<String,String> entry : URLS.entrySet() ) {
            String url = entry.getKey();
            String envName = entry.getValue();

            Object configValue = config.get(url);
            String value = null;

            if ( DEFAULT.equals(configValue) ) {
                value = scopedConfig.getUrl(agent, url);
            } else if ( configValue != null && ! StringUtils.isBlank(configValue.toString()) ) {
                value = configValue.toString();
            } else {
                value = String.format(LOCALHOST_URL_FORMAT.get(), connection.getCallbackPort());
            }

            data.put(envName, value);
        }

        StringBuilder line = new StringBuilder("export");
        for ( Map.Entry<String, String> entry : data.entrySet() ) {
            if ( line.length() > 0 ) {
                line.append(" ");
            }

            line.append(entry.getKey()).append("=").append(entry.getValue());
        }
        line.append(" ");
        line.append(ADDITIONAL_ENV.get().trim());
        line.append("\n");

        OutputStream os = connection.getExec().getInvertedIn();
        try {
            os.write(line.toString().getBytes("UTF-8"));
            os.flush();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }

        return true;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public EventService getEventService() {
        return eventService;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public SshAgentDao getAgentDao() {
        return agentDao;
    }

    @Inject
    public void setAgentDao(SshAgentDao agentDao) {
        this.agentDao = agentDao;
    }

    public KeyPairProvider getKeyPairProvider() {
        return keyPairProvider;
    }

    @Inject
    public void setKeyPairProvider(KeyPairProvider keyPairProvider) {
        this.keyPairProvider = keyPairProvider;
    }

    public ScopedConfig getScopedConfig() {
        return scopedConfig;
    }

    @Inject
    public void setScopedConfig(ScopedConfig scopedConfig) {
        this.scopedConfig = scopedConfig;
    }

}
