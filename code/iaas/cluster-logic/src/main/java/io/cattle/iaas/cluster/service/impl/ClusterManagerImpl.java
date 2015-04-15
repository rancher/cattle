package io.cattle.iaas.cluster.service.impl;

import static io.cattle.platform.core.model.tables.CertificateTable.CERTIFICATE;
import io.cattle.iaas.cluster.process.lock.CertificateAuthorityLock;
import io.cattle.iaas.cluster.service.ClusterManager;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.CertificateConstants;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.InstanceConstants.SystemContainer;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.ClusterHostMapDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.tables.records.ClusterHostMapRecord;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.idempotent.IdempotentRetryException;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.ssh.common.SshKeyGen;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;

public class ClusterManagerImpl implements ClusterManager {

    private static final Logger log = LoggerFactory.getLogger(ClusterManagerImpl.class);
    private static final DynamicLongProperty SSL_SOCAT_DEPLOY_TIMEOUT =
            DynamicPropertyFactory.getInstance().getLongProperty("ssl-socat.deploy.wait.time.millis", 60000);

    static final DynamicStringProperty CLUSTER_IMAGE_NAME = ArchaiusUtil.getString("cluster.image.name");
    static final DynamicStringListProperty CONFIG_ITEMS = ArchaiusUtil.getList("cluster.config.items");

    static final DynamicStringProperty CLUSTER_SECUREDOCKER_PORT = ArchaiusUtil.getString("cluster.securedocker.port");
    static final DynamicStringProperty SSL_AGENT_INSTANCE_NAME = ArchaiusUtil.getString("cluster.ssl.instance.name");
    static final DynamicStringProperty SSL_IMAGE_NAME = ArchaiusUtil.getString("cluster.ssl.image.name");
    static final DynamicStringListProperty SSL_CONFIG_ITEMS = ArchaiusUtil.getList("cluster.ssl.config.items");

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ConfigItemStatusManager statusManager;

    @Inject
    AgentInstanceFactory agentInstanceFactory;

    @Inject
    AgentInstanceDao agentInstanceDao;

    @Inject
    NetworkDao ntwkDao;

    @Inject
    AccountDao accountDao;

    @Inject
    IpAddressDao ipAddressDao;

    @Inject
    ObjectProcessManager processManager;

    @Inject
    ObjectManager objectManager;

    @Inject
    ClusterHostMapDao clusterHostMapDao;

    @Inject
    LockManager lockManager;

    @Override
    public Instance getClusterServerInstance(Host cluster) {
        Agent clusterServerAgent = getClusterServerAgent(cluster);
        Instance clusterServerInstance = null;
        if (clusterServerAgent != null) {
            clusterServerInstance = agentInstanceDao.getInstanceByAgent(clusterServerAgent);
        }
        return clusterServerInstance;
    }

    @Override
    public Agent getClusterServerAgent(Host cluster) {
        String uri = getUri(cluster);
        Agent clusterServerAgent = agentInstanceDao.getAgentByUri(uri);
        return clusterServerAgent;
    }

    private Instance createClusterServerInstance(Host cluster) {
        Instance clusterServerInstance = getClusterServerInstance(cluster);
        if (clusterServerInstance == null) {
            Host managingHost = getManagingHost(cluster);
            Integer clusterServerPort = DataAccessor.fields(cluster).withKey(ClusterConstants.CLUSTER_SERVER_PORT).as(Integer.class);

            Map<String, Object> params = new HashMap<>();
            params.put(InstanceConstants.FIELD_NETWORK_IDS, Lists.newArrayList(getNetworkIds(managingHost)));
            params.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, managingHost.getId());
            params.put(InstanceConstants.FIELD_PORTS, Lists.newArrayList(clusterServerPort + ":" + clusterServerPort + "/tcp"));

            clusterServerInstance = agentInstanceFactory
                    .newBuilder()
                    .withAccountId(managingHost.getAccountId())
                    .withZoneId(managingHost.getZoneId())
                    .withPrivileged(true)
                    .withUri(getUri(cluster, managingHost))
                    .withName(cluster.getName())
                    .withImageUuid(getImageUuid(cluster, managingHost))
                    .withParameters(params)
                    .withSystemContainerType(SystemContainer.ClusterAgent)
                    .build();
        } else {
            start(clusterServerInstance);
        }

        return clusterServerInstance;
    }

    private String getImageUuid(Host cluster, Host managingHost) {
        return getImagePrefix(cluster, managingHost) + CLUSTER_IMAGE_NAME.get();
    }

    private String getUri(Host cluster, Host managingHost) {
        return String.format("%s?clusterId=%d&managingHostId=%d",
                getConnectionPrefix(cluster, managingHost) + "///", cluster.getId(), managingHost.getId());
    }

    private String getUri(Host cluster) {
        return getUri(cluster, getManagingHost(cluster));
    }

    private String getConnectionPrefix(Host cluster, Host managingHost) {
        return objectManager.isKind(managingHost, "sim") ? "sim:" : "delegate:";
    }

    private String getImagePrefix(Host cluster, Host managingHost) {
        return objectManager.isKind(managingHost, "sim") ? "sim:" : "docker:";
    }

    private Long getNetworkIds(Host managingHost) {
        List<? extends Network> accountNetworks = ntwkDao.getNetworksForAccount(managingHost.getAccountId(),
                NetworkConstants.KIND_HOSTONLY);
        if (accountNetworks.isEmpty()) {
            // pass system network if account doesn't own any
            List<? extends Network> systemNetworks = ntwkDao.getNetworksForAccount(accountDao.getSystemAccount()
                    .getId(),
                    NetworkConstants.KIND_HOSTONLY);
            if (systemNetworks.isEmpty()) {
                throw new RuntimeException(
                        "Unable to find a network to start cluster server");
            }
            return systemNetworks.get(0).getId();
        }

        return accountNetworks.get(0).getId();
    }

    private void start(final Instance agentInstance) {
        if (InstanceConstants.STATE_STOPPED.equals(agentInstance.getState())) {
            DeferredUtils.nest(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    processManager.scheduleProcessInstance(InstanceConstants.PROCESS_START, agentInstance, null);
                    return null;
                }
            });
        }
    }

    @Override
    public Host getManagingHost(Host cluster) {
        Long managingHostId = DataAccessor.fields(cluster).withKey(ClusterConstants.MANAGING_HOST).as(Long.class);
        if (managingHostId == null) {
            throw new RuntimeException("Missing managingHostId for cluster:" + cluster.getId());
        }
        return objectManager.loadResource(Host.class, managingHostId);
    }

    @Override
    public void updateClusterServerConfig(ProcessState state, Host cluster) {
        if (!CommonStatesConstants.ACTIVE.equals(cluster.getState()) &&
                !CommonStatesConstants.ACTIVATING.equals(cluster.getState())) {
            return;
        }
        // short term optimization to avoid updating cluster object unnecessarily
        // since we're just currently only supporting file:// discoverySpec
        if (StringUtils.isEmpty(DataAccessor.fieldString(cluster, ClusterConstants.DISCOVERY_SPEC))) {
            DataUtils.getWritableFields(cluster).put(ClusterConstants.DISCOVERY_SPEC, "file:///etc/cluster/cluster-hosts.conf");
            objectManager.persist(cluster);
        }

        Instance clusterServerInstance = createClusterServerInstance(cluster);
        clusterServerInstance = resourceMonitor.waitFor(clusterServerInstance, new ResourcePredicate<Instance>() {
            @Override
            public boolean evaluate(Instance obj) {
                return InstanceConstants.STATE_RUNNING.equals(obj.getState());
            }
        });

        Agent clusterAgent = getClusterServerAgent(cluster);
        if (clusterAgent == null) {
            return;
        }
        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state,
                getContext(clusterAgent));
        request = updateClusterConfigBefore(request, clusterAgent);
        ConfigUpdateRequestUtils.setRequest(request, state, getContext(clusterAgent));
        updateClusterConfigAfter(request);
    }

    @Override
    public void activateCluster(ProcessState state, Host cluster) {
        Long currentManagingHostId = DataAccessor.fields(cluster).withKey(ClusterConstants.MANAGING_HOST).as(Long.class);
        Long hostId = findSuitableHost(cluster);
        if ((currentManagingHostId == null && hostId != null) ||
            (currentManagingHostId != null && hostId == null) ||
            (hostId != null && !hostId.equals(currentManagingHostId))) {
            DataUtils.getWritableFields(cluster).put(ClusterConstants.MANAGING_HOST, hostId);
            objectManager.persist(cluster);
        }

        if (hostId == null) {
            processManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, cluster, null);
        } else {
            try {
                createClusterCertificates(cluster);
                createClientCertificates(cluster);
            } catch (IdempotentRetryException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            updateClusterServerConfig(state, cluster);
        }

    }

    @Override
    public void checkSslAgentInstances(ProcessState state, Host cluster) {
        List<ClusterHostMapRecord> mappings = clusterHostMapDao.findClusterHostMapsForCluster(cluster);
        for (ClusterHostMapRecord mapping : mappings) {
            Host hostInCluster = objectManager.loadResource(Host.class, mapping.getHostId());
            // create server certificates
            if (hostInCluster.getCertificateId() == null) {
                try {
                    createDockerCertificates(hostInCluster);
                } catch (IdempotentRetryException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            deploySslAgent(hostInCluster);
            updateSslConfig(state, hostInCluster);
        }
    }

    @SuppressWarnings("deprecation")
    private void createDockerCertificates(Host host) throws Exception {
        Certificate caCertRecord = getOrCreateCaCertificates(host.getAccountId());

        Certificate serverCertRecord;
        if (host.getCertificateId() != null) {
            serverCertRecord = objectManager.loadResource(Certificate.class, host.getCertificateId());
            if (serverCertRecord != null &&
                    serverCertRecord.getCertChain() != null &&
                    serverCertRecord.getCertChain().equals(caCertRecord.getCert())) {
                // docker ssl cert for host currently exist and matches the current certificate
                // authority
                return;
            }
        }

        KeyPair caKeyPair = SshKeyGen.readKeyPair(caCertRecord.getKey());

        KeyPair serverKeyPair = SshKeyGen.generateKeyPair();
        String serverKey = SshKeyGen.writePemObject(serverKeyPair.getPrivate());

        IpAddress ipAddress = clusterHostMapDao.getIpAddressForHost(host.getId());

        PKCS10CertificationRequest serverCSR = new PKCS10CertificationRequest(
                "SHA256withRSA",
                new X500Principal("CN="),
                serverKeyPair.getPublic(),
                null,
                serverKeyPair.getPrivate());
        X509V3CertificateGenerator serverCertGen = new X509V3CertificateGenerator();
        serverCertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        serverCertGen.setIssuerDN(CertificateConstants.CERT_ISSUER);
        serverCertGen.setNotBefore(new Date());
        serverCertGen.setNotAfter(new Date(System.currentTimeMillis() + CertificateConstants.EXPIRATION_MILLIS.get()));
        serverCertGen.setSubjectDN(serverCSR.getCertificationRequestInfo().getSubject());
        serverCertGen.setPublicKey(serverCSR.getPublicKey());
        serverCertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");

        serverCertGen.addExtension(X509Extensions.ExtendedKeyUsage, false, new ExtendedKeyUsage(
                KeyPurposeId.id_kp_serverAuth));
        GeneralNames subjectAltName = new GeneralNames(new DERSequence(new GeneralName[] {
                new GeneralName(GeneralName.iPAddress, ipAddress.getAddress()),
                new GeneralName(GeneralName.iPAddress, "127.0.0.1")
        }));
        serverCertGen.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName);

        X509Certificate serverCrt = serverCertGen.generateX509Certificate(caKeyPair.getPrivate());
        String serverCert = SshKeyGen.writePemObject(serverCrt);

        // create cert record
        Map<Object, Object> data = new HashMap<>();
        data.put(CERTIFICATE.NAME, CertificateConstants.SERVER);
        data.put(CERTIFICATE.KEY, serverKey);
        data.put(CERTIFICATE.CERT, serverCert);
        data.put(CERTIFICATE.CERT_CHAIN, caCertRecord.getCert());
        data.put(CERTIFICATE.ACCOUNT_ID, host.getAccountId());
        serverCertRecord = objectManager.create(Certificate.class, objectManager.convertToPropertiesFor(Certificate.class, data));

        // update host with cert id
        host.setCertificateId(serverCertRecord.getId());
        objectManager.persist(host);
    }

    @SuppressWarnings("deprecation")
    private void createClusterCertificates(Host cluster) throws Exception {
        Certificate caCertRecord = getOrCreateCaCertificates(cluster.getAccountId());

        Certificate clusterCertRecord = objectManager.findOne(Certificate.class,
                CERTIFICATE.ACCOUNT_ID, cluster.getAccountId(),
                CERTIFICATE.NAME, CertificateConstants.CLUSTER,
                CERTIFICATE.CERT_CHAIN, caCertRecord.getCert(),
                CERTIFICATE.REMOVED, null);

        if (clusterCertRecord == null) {
            KeyPair caKeyPair = SshKeyGen.readKeyPair(caCertRecord.getKey());

            KeyPair swarmKeyPair = SshKeyGen.generateKeyPair();
            String swarmKey = SshKeyGen.writePemObject(swarmKeyPair.getPrivate());

            Host managingHost = getManagingHost(cluster);
            IpAddress ipAddress = clusterHostMapDao.getIpAddressForHost(managingHost.getId());

            PKCS10CertificationRequest swarmCSR = new PKCS10CertificationRequest(
                    "SHA256withRSA",
                    new X500Principal("CN="),
                    swarmKeyPair.getPublic(),
                    null,
                    swarmKeyPair.getPrivate());
            X509V3CertificateGenerator swarmCertGen = new X509V3CertificateGenerator();
            swarmCertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
            swarmCertGen.setIssuerDN(CertificateConstants.CERT_ISSUER);
            swarmCertGen.setNotBefore(new Date());
            swarmCertGen.setNotAfter(new Date(System.currentTimeMillis() + CertificateConstants.EXPIRATION_MILLIS.get()));
            swarmCertGen.setSubjectDN(swarmCSR.getCertificationRequestInfo().getSubject());
            swarmCertGen.setPublicKey(swarmCSR.getPublicKey());
            swarmCertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");

            swarmCertGen.addExtension(X509Extensions.ExtendedKeyUsage, false, new ExtendedKeyUsage(
                    new DERSequence(new KeyPurposeId[] { KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth})));

            GeneralNames subjectAltName = new GeneralNames(new DERSequence(new GeneralName[] {
                    new GeneralName(GeneralName.iPAddress, ipAddress.getAddress()),
                    new GeneralName(GeneralName.iPAddress, "127.0.0.1")
            }));
            swarmCertGen.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName);

            X509Certificate swarmCrt = swarmCertGen.generateX509Certificate(caKeyPair.getPrivate());
            String swarmCert = SshKeyGen.writePemObject(swarmCrt);

            // create cert record
            Map<Object, Object> data = new HashMap<>();
            data.put(CERTIFICATE.KEY, swarmKey);
            data.put(CERTIFICATE.CERT, swarmCert);
            data.put(CERTIFICATE.CERT_CHAIN, caCertRecord.getCert());
            data.put(CERTIFICATE.ACCOUNT_ID, cluster.getAccountId());
            data.put(CERTIFICATE.NAME, CertificateConstants.CLUSTER);
            clusterCertRecord = objectManager.create(Certificate.class, objectManager.convertToPropertiesFor(Certificate.class, data));
        }

        // update host with cert id
        cluster.setCertificateId(clusterCertRecord.getId());
    }

    @SuppressWarnings("deprecation")
    private void createClientCertificates(Host cluster) throws Exception {
        Certificate caCertRecord = getOrCreateCaCertificates(cluster.getAccountId());

        Certificate clientCertRecord = objectManager.findOne(Certificate.class,
                CERTIFICATE.ACCOUNT_ID, cluster.getAccountId(),
                CERTIFICATE.NAME, CertificateConstants.CLIENT,
                CERTIFICATE.CERT_CHAIN, caCertRecord.getCert(),
                CERTIFICATE.REMOVED, null);

        if (clientCertRecord == null) {
            KeyPair caKeyPair = SshKeyGen.readKeyPair(caCertRecord.getKey());

            KeyPair clientKeyPair = SshKeyGen.generateKeyPair();
            String clientKey = SshKeyGen.writePemObject(clientKeyPair.getPrivate());

            PKCS10CertificationRequest clientCSR = new PKCS10CertificationRequest(
                    "SHA256withRSA",
                    new X500Principal("CN=client"),
                    clientKeyPair.getPublic(),
                    null,
                    clientKeyPair.getPrivate());
            X509V3CertificateGenerator clientCertGen = new X509V3CertificateGenerator();
            clientCertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
            clientCertGen.setIssuerDN(CertificateConstants.CERT_ISSUER);
            clientCertGen.setNotBefore(new Date());
            clientCertGen.setNotAfter(new Date(System.currentTimeMillis() + CertificateConstants.EXPIRATION_MILLIS.get()));
            clientCertGen.setSubjectDN(clientCSR.getCertificationRequestInfo().getSubject());
            clientCertGen.setPublicKey(clientCSR.getPublicKey());
            clientCertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");

            clientCertGen.addExtension(X509Extensions.ExtendedKeyUsage, false, new ExtendedKeyUsage(
                    KeyPurposeId.id_kp_clientAuth));
            X509Certificate clientCrt = clientCertGen.generateX509Certificate(caKeyPair.getPrivate());
            String clientCert = SshKeyGen.writePemObject(clientCrt);

            // create cert record
            Map<Object, Object> data = new HashMap<>();
            data.put(CERTIFICATE.KEY, clientKey);
            data.put(CERTIFICATE.CERT, clientCert);
            data.put(CERTIFICATE.CERT_CHAIN, caCertRecord.getCert());
            data.put(CERTIFICATE.NAME, CertificateConstants.CLIENT);
            data.put(CERTIFICATE.ACCOUNT_ID, caCertRecord.getAccountId());
            objectManager.create(Certificate.class, objectManager.convertToPropertiesFor(Certificate.class, data));
        }
    }

    private Certificate getOrCreateCaCertificates(final Long accountId) throws Exception {
        return lockManager.lock(new CertificateAuthorityLock(accountId), new LockCallback<Certificate>() {
            @Override
            public Certificate doWithLock() {
                Certificate ca = objectManager.findOne(Certificate.class,
                        CERTIFICATE.ACCOUNT_ID, accountId,
                        CERTIFICATE.NAME, CertificateConstants.CA,
                        CERTIFICATE.REMOVED, null);
                if (ca != null) {
                    return ca;
                }

                try {
                    KeyPair caKeyPair = SshKeyGen.generateKeyPair();
                    String caPrivateKey = SshKeyGen.writePemObject(caKeyPair.getPrivate());

                    X509V3CertificateGenerator caCertGen = new X509V3CertificateGenerator();
                    caCertGen.setPublicKey(caKeyPair.getPublic());
                    caCertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
                    caCertGen.setNotBefore(new Date());
                    caCertGen.setNotAfter(new Date(System.currentTimeMillis() + CertificateConstants.EXPIRATION_MILLIS.get()));
                    caCertGen.setIssuerDN(CertificateConstants.CERT_ISSUER);
                    caCertGen.setSubjectDN(CertificateConstants.CERT_ISSUER);
                    caCertGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

                    X509Certificate caCrt = caCertGen.generateX509Certificate(caKeyPair.getPrivate());

                    PKCS10CertificationRequest caCSR = new PKCS10CertificationRequest(
                            "SHA256withRSA",
                            caCrt.getSubjectX500Principal(),
                            caKeyPair.getPublic(),
                            null,
                            caKeyPair.getPrivate());

                    caCertGen.addExtension(X509Extensions.SubjectKeyIdentifier,
                            false, new SubjectKeyIdentifierStructure(caCSR.getPublicKey()));
                    caCertGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                            new AuthorityKeyIdentifierStructure(caCrt));
                    caCertGen.addExtension(X509Extensions.BasicConstraints, false, new BasicConstraints(true));

                    caCrt = caCertGen.generateX509Certificate(caKeyPair.getPrivate());
                    String caCert = SshKeyGen.writePemObject(caCrt);

                    Map<Object, Object> data = new HashMap<>();
                    data.put(CERTIFICATE.NAME, CertificateConstants.CA);
                    data.put(CERTIFICATE.KEY, caPrivateKey);
                    data.put(CERTIFICATE.CERT, caCert);
                    data.put(CERTIFICATE.ACCOUNT_ID, accountId);
                    ca = objectManager.create(Certificate.class, objectManager.convertToPropertiesFor(Certificate.class, data));

                    return ca;
                } catch (IOException | InvalidKeyException | SecurityException | SignatureException |
                        NoSuchAlgorithmException | NoSuchProviderException | CertificateParsingException e) {
                    throw new RuntimeException("Unable to generate certificate authority", e);
                }
            }
        });
    }

    private String getConnectionPrefix(Host host) {
        return objectManager.isKind(host, "sim") ? "sim:" : "delegate:";
    }

    private String getImagePrefix(Host host) {
        return objectManager.isKind(host, "sim") ? "sim:" : "docker:";
    }

    private String getSslAgentUri(Host host) {
        return String.format("%s?hostId=%d&ssl=true",
                getConnectionPrefix(host) + "///",
                host.getId());
    }

    private Instance getSslAgentInstance(Host host) {
        Agent sslAgent = agentInstanceDao.getAgentByUri(getSslAgentUri(host));
        Instance sslAgentInstance = null;
        if (sslAgent != null) {
            sslAgentInstance = agentInstanceDao.getInstanceByAgent(sslAgent);
        }
        return sslAgentInstance;
    }

    private void deploySslAgent(Host host) {
        log.trace("Deploying socat agent");
        Instance sslAgentInstance = getSslAgentInstance(host);
        if (sslAgentInstance == null) {
            Map<String, Object> params = new HashMap<>();
            params.put(InstanceConstants.FIELD_NETWORK_IDS, Lists.newArrayList(getNetworkIds(host)));
            params.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, host.getId());
            params.put(InstanceConstants.FIELD_PORTS,
                    Lists.newArrayList(
                            CLUSTER_SECUREDOCKER_PORT.get() + ":" + CLUSTER_SECUREDOCKER_PORT.get() + "/tcp"));
            params.put(InstanceConstants.FIELD_DATA_VOLUMES, Lists.newArrayList("/var/run/docker.sock:/var/run/docker.sock"));

            sslAgentInstance = agentInstanceFactory
                    .newBuilder()
                    .withAccountId(host.getAccountId())
                    .withZoneId(host.getZoneId())
                    .withPrivileged(true)
                    .withUri(getSslAgentUri(host))
                    .withName(SSL_AGENT_INSTANCE_NAME.get())
                    .withImageUuid(getImagePrefix(host) + SSL_IMAGE_NAME.get())
                    .withParameters(params)
                    .withSystemContainerType(SystemContainer.SslAgent)
                    .build();
        } else {
            start(sslAgentInstance);
        }
    }

    private void updateSslConfig(ProcessState state, Host host) {
        log.trace("Attempting to update socat config");
        Instance sslAgentInstance = getSslAgentInstance(host);
        sslAgentInstance = resourceMonitor.waitFor(sslAgentInstance, SSL_SOCAT_DEPLOY_TIMEOUT.get(), new ResourcePredicate<Instance>() {
            @Override
            public boolean evaluate(Instance obj) {
                return InstanceConstants.STATE_RUNNING.equals(obj.getState());
            }
        });
        log.trace("socat instance should be running.  Trying to update config now");
        Agent sslAgent = agentInstanceDao.getAgentByUri(getSslAgentUri(host));
        if (sslAgent == null) {
            return;
        }
        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state,
                getContext(sslAgent));
        request = updateSslConfigBefore(request, sslAgent);
        ConfigUpdateRequestUtils.setRequest(request, state, getContext(sslAgent));
        updateSslConfigAfter(request);
        log.trace("socat update config request completed:" + request);
    }

    private ConfigUpdateRequest updateSslConfigBefore(ConfigUpdateRequest request, Agent agent) {
        if (request == null) {
            request = new ConfigUpdateRequest(agent.getId());
            for (String item : SSL_CONFIG_ITEMS.get()) {
                request.addItem(item)
                        .withApply(true)
                        .withIncrement(true)
                        .setCheckInSyncOnly(true);
            }
        }
        statusManager.updateConfig(request);
        return request;
    }

    private void updateSslConfigAfter(ConfigUpdateRequest request) {
        if (request == null) {
            return;
        }
        statusManager.waitFor(request);
    }

    private Long findSuitableHost(Host cluster) {
        List<ClusterHostMapRecord> mappings = clusterHostMapDao.findClusterHostMapsForCluster(cluster);
        if (mappings.size() == 0) {
            return null;
        }
        Long currentManagingHostId = DataAccessor.fields(cluster).withKey(ClusterConstants.MANAGING_HOST).as(Long.class);
        Host host = objectManager.loadResource(Host.class, currentManagingHostId);
        if (host != null && CommonStatesConstants.ACTIVE.equals(host.getState())) {
            for (ClusterHostMapRecord mapping: mappings) {
                if (mapping.getHostId() == currentManagingHostId) {
                    return currentManagingHostId;
                }
            }
        }
        for (ClusterHostMapRecord mapping: mappings) {
            host = objectManager.loadResource(Host.class, mapping.getHostId());
            if (host != null && CommonStatesConstants.ACTIVE.equals(host.getState())) {
                return mapping.getHostId();
            }
        }
        return null;
    }

    private ConfigUpdateRequest updateClusterConfigBefore(ConfigUpdateRequest request, Agent agent) {
        if (request == null) {
            request = new ConfigUpdateRequest(agent.getId());
            for (String item : CONFIG_ITEMS.get()) {
                request.addItem(item)
                        .withApply(true)
                        .withIncrement(true)
                        .setCheckInSyncOnly(true);
            }
        }
        statusManager.updateConfig(request);
        return request;
    }

    private void updateClusterConfigAfter(ConfigUpdateRequest request) {
        if (request == null) {
            return;
        }
        statusManager.waitFor(request);
    }

    private String getContext(Agent agent) {
        return String.format("AgentUpdateConfig:%s", agent.getId());
    }

}
