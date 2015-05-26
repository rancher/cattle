openssl-listen:${securePort},reuseaddr,cert=/etc/ssl-socat/server.pem,cafile=/etc/ssl-socat/ca.crt,fork UNIX:/var/run/docker.sock
