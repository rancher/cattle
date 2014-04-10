package io.cattle.platform.util.net;

public class NetUtils {

    public static long ip2Long(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("[.]");

        long result = 0;

        for (int i = 3; i >= 0; i--) {
            long ip = Long.parseLong(ipAddressInArray[3 - i]);
            result |= ip << (i * 8);
        }

        return result;
    }

    public static String long2Ip(long ip) {
        return ((ip >> 24) & 0xFF) + "."
                + ((ip >> 16) & 0xFF) + "."
                + ((ip >> 8) & 0xFF) + "."
                + (ip & 0xFF);
    }

    public static String getDefaultGateway(String ipAddress, int cidrSize) {
        long ip = ip2Long(ipAddress);
        long mask = (long)Math.pow(2, 32-cidrSize) - 1;

        long gateway = (ip & ~mask) + 1;

        return long2Ip(gateway);
    }

    public static String getDefaultStartAddress(String ipAddress, int cidrSize) {
        long ip = ip2Long(ipAddress);
        long mask = (long)Math.pow(2, 32-cidrSize) - 1;

        long start = (ip & ~mask) + 2;

        return long2Ip(start);
    }

    public static String getDefaultEndAddress(String ipAddress, int cidrSize) {
        long ip = ip2Long(ipAddress);
        long mask = (long)Math.pow(2, 32-cidrSize) - 1;

        long end = (ip & ~mask) + (long)Math.pow(2, 32-cidrSize) - 6;

        return long2Ip(end);
    }

}