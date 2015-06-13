package io.cattle.platform.server.context;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringProperty;

public class ServerContext {

    public static final DynamicIntProperty HTTP_PORT = ArchaiusUtil.getInt("cattle.http.port");
    public static final DynamicIntProperty HTTPS_PORT = ArchaiusUtil.getInt("cattle.https.port");
    public static final DynamicStringProperty URL_PATH = ArchaiusUtil.getString("cattle.url.path");
    public static final DynamicStringProperty SERVER_IP = ArchaiusUtil.getString("cattle.server.ip");
    public static final DynamicStringProperty SERVER_ID = ArchaiusUtil.getString("cattle.server.id");
    public static final DynamicStringProperty HOST = ArchaiusUtil.getString("api.host");

    private static final String URL_SETTING_FORMAT = "cattle.%s.url";
    private static final String DEFAULT_URL = "public.url";
    private static final String FOUND_SERVER_IP = lookupServerIp();
    private static final String SERVER_ID_FORMAT = System.getProperty("cattle.server.id.format", "%s");

    public static final String HOST_API_PROXY_MODE_OFF = "off";
    public static final String HOST_API_PROXY_MODE_EMBEDDED = "embedded";
    public static final String HOST_API_PROXY_MODE_HA = "ha";

    public static boolean isCustomApiHost() {
        return !StringUtils.isBlank(HOST.get());
    }

    public static ServerAddress getServerAddress() {
        return getServerAddress(null);
    }

    public static ServerAddress getServerAddress(String name) {
        return getServerAddress(null, name);
    }

    public static ServerAddress getServerAddress(String scope, String name) {
        String url = null;

        if (scope != null) {
            url = ArchaiusUtil.getString(String.format(URL_SETTING_FORMAT, scope + "." + name)).get();
        }

        if (url == null) {
            url = ArchaiusUtil.getString(String.format(URL_SETTING_FORMAT, name)).get();
        }

        if (url == null) {
            url = ArchaiusUtil.getString(DEFAULT_URL).get();
        }

        if (url == null) {
            String apiHost = HOST.get();
            if (StringUtils.isNotBlank(apiHost)) {
                if (apiHost.startsWith("http")) {
                    return new ServerAddress(apiHost + URL_PATH.get());
                } else {
                    return new ServerAddress("http://" + apiHost + URL_PATH.get());
                }
            }
        }

        if (url == null) {
            StringBuilder buffer = new StringBuilder();
            if (HTTPS_PORT.get() > 0) {
                buffer.append("https://");
                buffer.append(getServerIp());
                buffer.append(":").append(HTTPS_PORT.get());
            } else {
                buffer.append("http://");
                buffer.append(getServerIp());
                buffer.append(":").append(HTTP_PORT.get());
            }

            buffer.append(URL_PATH.get());

            url = buffer.toString();
        }

        return new ServerAddress(url);
    }

    public static String getServerId() {
        String id = SERVER_ID.get();
        String ip = getServerIp();

        if (id != null) {
            return String.format(id, ip);
        }

        return String.format(SERVER_ID_FORMAT, ip);
    }

    public static String getHostApiProxyMode() {
        String embedded = System.getenv("CATTLE_HOST_API_PROXY_MODE");
        if (StringUtils.isEmpty(embedded)) {
            embedded = System.getProperty("host.api.proxy.mode", "off");
        }
        return embedded;
    }

    protected static String getServerIp() {
        String ip = SERVER_IP.get();
        return ip == null ? FOUND_SERVER_IP : ip;
    }

    protected static String lookupServerIp() {
        String address = null;
        String v6Address = null;

        try {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (addr instanceof Inet6Address) {
                        v6Address = addr.getHostAddress();
                    } else {
                        if (!addr.isLoopbackAddress() && (address == null || !addr.isSiteLocalAddress())) {
                            address = addr.getHostAddress();
                        }
                    }
                }
            }

            if (address != null) {
                return address;
            } else if (v6Address != null) {
                return v6Address;
            } else {
                return "localhost";
            }
        } catch (SocketException e) {
            throw new IllegalStateException("Failed to lookup IP of server", e);
        }
    }

}
