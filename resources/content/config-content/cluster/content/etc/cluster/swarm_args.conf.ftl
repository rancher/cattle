manage ${discoverySpec} --tlsverify --tlscacert=/etc/cluster/ca.crt --tlscert=/etc/cluster/cluster.crt --tlskey=/etc/cluster/cluster.key --strategy random -host=0.0.0.0:${clusterServerPort}
