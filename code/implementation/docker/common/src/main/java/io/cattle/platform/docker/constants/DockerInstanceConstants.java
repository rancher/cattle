package io.cattle.platform.docker.constants;

public class DockerInstanceConstants {

    public static final String FIELD_DOCKER_PORTS = "dockerPorts";
    public static final String FIELD_DOCKER_HOST_IP = "dockerHostIp";
    public static final String FIELD_DOCKER_IP = "dockerIp";
    public static final String FIELD_VOLUMES_FROM = "dataVolumesFrom";
    public static final String EVENT_FIELD_VOLUMES_FROM = "dataVolumesFromContainers";

    public static final String DOCKER_ATTACH_STDIN = "AttachStdin";
    public static final String DOCKER_ATTACH_STDOUT = "AttachStdout";
    public static final String DOCKER_TTY = "Tty";
    public static final String DOCKER_CMD = "Cmd";
    public static final String DOCKER_CONTAINER = "Container";
    
    public static final String CONTAINER_LOGS_FOLLOW = "follow";
    public static final String CONTAINER_LOGS_STDOUT = "StdOut";
    public static final String CONTAINER_LOGS_STDERR = "StdErr";
    public static final String CONTAINER_LOGS_TIMESTAMP = "timeStamp";
    public static final String CONTAINER_LOGS_TAIL = "lines";

}