package io.github.ibuildthecloud.agent.server.connection.ssh;

import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.agent.server.connection.AgentConnectionFactory;
import io.github.ibuildthecloud.dstack.core.model.Agent;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.SshdSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshAgentConnectionFactory implements AgentConnectionFactory {

    private static Logger log = LoggerFactory.getLogger(SshAgentConnectionFactory.class);

    private static Pattern SSH_PATTERN = Pattern.compile("ssh://"
                                            + "([^:]+)(:([^@]+))?@"
                                            + "([^:]+)(:([0-9]+))?");
    private static String PROTOCOL = "ssh://";

    @Override
    public AgentConnection createConnection(Agent agent) {
        String uri = agent.getUri();
        if ( uri == null || ! uri.startsWith(PROTOCOL) ) {
            return null;
        }


        SshConnectionOptions opts = parseOpts(uri);
        if ( opts == null ) {
            return null;
        }

        try {
            return createConnection(opts);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
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

    protected AgentConnection createConnection(SshConnectionOptions opts) throws IOException, InterruptedException {
        SshClient client = SshClient.setUpDefaultClient();
        client.setTcpipForwarderFactory(new DefaultTcpipForwarderFactory());
        client.setTcpipForwardingFilter(new ReverseConnectAllowFilter());

        client.start();
        ConnectFuture connect = client.connect(opts.getHost(), opts.getPort());
        connect.await();
        ClientSession session = connect.getSession();

        session.authPassword(opts.getUsername(), opts.getPassword());

        int ret = session.waitFor(ClientSession.WAIT_AUTH | ClientSession.CLOSED | ClientSession.AUTHED, 0);

        if ((ret & ClientSession.CLOSED) != 0) {
            System.err.println("error");
            System.exit(-1);
        }

        session.startRemotePortForwarding(new SshdSocketAddress("localhost", 12345), new SshdSocketAddress("localhost", 8080));
//        ChannelDirectTcpip tcpip = session.createDirectTcpipChannel(new SshdSocketAddress("localhost", 8080), );
//        tcpip.open().await(5000);

        return null;
    }

}
