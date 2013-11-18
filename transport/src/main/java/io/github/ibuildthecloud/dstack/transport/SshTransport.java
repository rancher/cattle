package io.github.ibuildthecloud.dstack.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshTransport implements Transport {

    private static final Logger log = LoggerFactory.getLogger(SshTransport.class);

    String hostname, username, password;
    int port;
    Session session;
    Channel channel;
    boolean connected = false;

    public SshTransport(String hostname, String username, String password, int port) {
        super();
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.port = port;
    }

    @Override
    public synchronized void connect() throws IOException {
        if ( connected ) {
            return;
        }

        try {
            log.error("Connecting to SSH [{}@{}:{}]", username, hostname, port);
            session = new JSch().getSession(username, hostname, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand("/var/lib/dstack/agent");
            channel.connect();
            connected = true;
        } catch (JSchException e) {
            log.error("Failed to connect to SSH [{}@{}:{}]", username, hostname, port);
            throw new IOException(e);
        }
    }

    @Override
    public synchronized void disconnect() {
        if ( connected ) {
            log.error("Closing SSH connection to [{}@{}:{}]", username, hostname, port);
            connected = false;
            if ( channel != null ) {
                channel.disconnect();
            }
            if ( session != null ) {
                session.disconnect();
            }
        }
    }

    @Override
    public String send(String data) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        OutputStream os = channel.getOutputStream();
        os.write((data + "\n").getBytes("UTF-8"));
        os.flush();

        while ( true ) {
            String line = reader.readLine();
            log.info("SSH Output : {}", line);
            if ( line.startsWith("{") ) {
                return line;
            }
        }
    }

}