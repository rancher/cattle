package io.cattle.platform.agent.connection.ssh;

public class SshConnectionOptions {

    String host;
    int port;
    String username;
    String password;

    public SshConnectionOptions(String host, String port, String username, String password) {
        super();
        this.host = host;
        this.port = port == null ? 22 : Integer.parseInt(port);
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password == null ? username : password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return username + "@" + host + ":" + port;
    }

}
