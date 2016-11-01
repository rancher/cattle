package io.cattle.platform.docker.client;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class DockerImage {

    public static final Integer NAME_TOTAL_LENGTH_MAX = 255;

    public static final String DEFAULT_REGISTRY = "index.docker.io";

    public static final String KIND_PREFIX = "docker:";
    
    public static final String SIM_PREFIX = "sim:";

    String fullName, serverAddress;

    public DockerImage(String fullName, String serverAddress) {
        super();
        this.fullName = fullName;
        this.serverAddress = serverAddress;
    }

    public static DockerImage parse(String uuid) {
        // if we encounter an error we will just return the raw string and
        // default registry
        boolean flag = false;
        // only strip once
        if (uuid.startsWith(KIND_PREFIX)) {
            uuid = uuid.substring(7);
            flag = true;
        }
        if (uuid.startsWith("sim:") && !flag) {
            uuid = uuid.substring(4);
        }
        String[] subMatches = findAllSubMatches(uuid, Regexp.REFERENCE_REGEXP);
        if (subMatches.length == 0) {
            if (uuid == "") {
                return null;
            }
            String[] subMatches2 = findAllSubMatches(uuid.toLowerCase(), Regexp.REFERENCE_REGEXP);
            if (subMatches2.length == 0) {
                return null;
            }
            return null;
        }

        if (subMatches[1].length() > NAME_TOTAL_LENGTH_MAX) {
            return null;
        }

        String name = subMatches[1];
        String fullname = uuid;
        String hostName = resolveHostName(name);
        if (fullname.startsWith(hostName + "/")) {
            fullname = StringUtils.removeStart(fullname, hostName + "/");
        }
        return new DockerImage(fullname, hostName);
    }

    public String getFullName() {
        return this.fullName;
    }

    public String getServer() {
        return this.serverAddress;
    }

    public static String[] findAllSubMatches(String s, Pattern p) {
        ArrayList<String> result = new ArrayList<>();
        Matcher matcher = p.matcher(s);
        if (matcher.find()) {
            result.add(matcher.group());
            for (int j = 1; j < matcher.groupCount() + 1; j++) {
                String ret = matcher.group(j);
                if (ret != null) {
                    result.add(matcher.group(j));
                }
            }
        }

        return result.toArray(new String[result.size()]);
    }
    
    public static String resolveHostName(String name) {
        String hostname = "";
        int i = name.indexOf("/");
        if (i == -1 || (!(name.substring(0, i).contains(".") || name.substring(0, i).contains(":"))
                && !name.substring(0, i).equals("localhost"))) {
            return DEFAULT_REGISTRY;
        } 
        else {
            hostname = name.substring(0, i);
        }
        return hostname;
    }
}
