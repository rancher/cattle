package io.cattle.platform.app.components;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.impl.AgentLocatorImpl;
import io.cattle.platform.agent.instance.serialization.AgentInstanceAuthObjectPostProcessor;
import io.cattle.platform.api.resource.DefaultResourceManagerSupport;
import io.cattle.platform.api.resource.ObjectResourceManagerSupport;
import io.cattle.platform.api.resource.jooq.JooqAccountAuthorization;
import io.cattle.platform.api.resource.jooq.JooqResourceListSupport;
import io.cattle.platform.compose.export.ComposeExportService;
import io.cattle.platform.compose.export.impl.ComposeExportServiceImpl;
import io.cattle.platform.compose.export.impl.RancherCertificatesToComposeFormatter;
import io.cattle.platform.compose.export.impl.RancherGenericMapToComposeFormatter;
import io.cattle.platform.compose.export.impl.RancherImageToComposeFormatter;
import io.cattle.platform.docker.process.serializer.DockerContainerSerializer;
import io.cattle.platform.docker.transform.DockerTransformer;
import io.cattle.platform.docker.transform.DockerTransformerImpl;
import io.cattle.platform.engine.process.ProcessRouter;
import io.cattle.platform.framework.encryption.handler.impl.TransformationServiceImpl;
import io.cattle.platform.framework.encryption.impl.Aes256Encrypter;
import io.cattle.platform.framework.encryption.impl.PasswordHasher;
import io.cattle.platform.framework.encryption.impl.Sha256Hasher;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.framework.secret.SecretsServiceImpl;
import io.cattle.platform.host.service.HostApiRSAKeyProvider;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.host.service.impl.HostApiServiceImpl;
import io.cattle.platform.iaas.api.auditing.AuditService;
import io.cattle.platform.iaas.api.auditing.AuditServiceImpl;
import io.cattle.platform.iaas.api.service.CertificateService;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.iaas.api.service.impl.CertificateServiceImpl;
import io.cattle.platform.object.serialization.ObjectSerializerFactory;
import io.cattle.platform.object.serialization.impl.DefaultObjectSerializerFactoryImpl;
import io.cattle.platform.register.auth.RegistrationAuthTokenManager;
import io.cattle.platform.register.auth.impl.RegistrationAuthTokenManagerImpl;
import io.cattle.platform.service.account.SystemRoleObjectPostProcessor;
import io.cattle.platform.service.launcher.ServiceAccountCreateStartup;
import io.cattle.platform.service.revision.impl.RevisionManagerImpl;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.storage.service.impl.StorageServiceImpl;
import io.cattle.platform.task.Task;
import io.cattle.platform.task.impl.TaskManagerImpl;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.token.impl.JwtTokenServiceImpl;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRouter;
import io.github.ibuildthecloud.gdapi.request.impl.ApiRouterImpl;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Common {

    AgentLocator agentLocator;
    ApiRouter router;
    AuditService auditService;
    CertificateService certService;
    ComposeExportService composeExportService;
    DefaultResourceManagerSupport support;
    DockerTransformer dockerTransformer;
    HostApiService hostApiService;
    HostApiRSAKeyProvider keyProvider;
    ObjectSerializerFactory objectSerializerFactory;
    ProcessRouter processes;
    RegistrationAuthTokenManager registrationAuthTokenManager;
    ResourceManagerLocator locator;
    RevisionManager revisionManager;
    SecretsService secretsService;
    ServiceAccountCreateStartup serviceAccountCreateStartup;
    StorageService storageService;
    TaskManagerImpl taskManager;
    TokenService tokenService;
    TransformationServiceImpl transformationService;

    List<Task> tasks = new ArrayList<>();
    Map<String, SchemaFactory> schemaFactories = new HashMap<>();

    public Common(Framework f, DataAccess d) {
        super();
        this.processes = f.processRegistry;
        this.transformationService = d.transformationService;
        this.keyProvider = new HostApiRSAKeyProvider(d.dataDao);
        this.tokenService = new JwtTokenServiceImpl(keyProvider);
        this.hostApiService = new HostApiServiceImpl(f.objectManager, tokenService, keyProvider);
        this.registrationAuthTokenManager = new RegistrationAuthTokenManagerImpl(d.registrationTokenAuthDao, f.objectManager, d.accountDao);
        this.secretsService = new SecretsServiceImpl(d.secretsDao, f.jsonMapper, keyProvider);
        this.auditService = new AuditServiceImpl(d.auditLogDao, f.schemaJsonMapper, f.idFormatter);
        this.agentLocator = new AgentLocatorImpl(d.agentDao, f.objectManager, f.eventService, f.jsonMapper);
        this.storageService = new StorageServiceImpl(f.objectManager, d.resourceDao, f.lockManager, d.storagePoolDao);
        this.revisionManager = new RevisionManagerImpl(f.objectManager, f.processManager, d.serviceDao, d.resourceDao, f.coreSchemaFactory, storageService);
        this.dockerTransformer = new DockerTransformerImpl(f.jsonMapper);
        this.composeExportService = new ComposeExportServiceImpl(f.objectManager, d.serviceConsumeMapDao, d.loadBalancerInfoDao,
                Arrays.asList(
                        new RancherCertificatesToComposeFormatter(f.jooqConfig, f.objectManager),
                        new RancherImageToComposeFormatter(),
                        new RancherGenericMapToComposeFormatter()));
        this.certService = new CertificateServiceImpl(f.objectManager, keyProvider, d.dataDao);
        this.taskManager = new TaskManagerImpl(f.scheduledExecutorService, f.eventService, d.taskDao, tasks);
        this.serviceAccountCreateStartup = new ServiceAccountCreateStartup(f.lockManager, f.lockDelegator, f.scheduledExecutorService, d.accountDao, d.resourceDao, f.resourceMonitor, f.processManager);
        this.objectSerializerFactory = new DefaultObjectSerializerFactoryImpl(f.jsonMapper, f.objectManager, f.metaDataManager,
                new DockerContainerSerializer(f.objectManager),
                new AgentInstanceAuthObjectPostProcessor(f.objectManager),
                new SystemRoleObjectPostProcessor(f.objectManager, serviceAccountCreateStartup));

        ObjectResourceManagerSupport objSupport = new ObjectResourceManagerSupport(f.objectManager, f.processManager);
        JooqAccountAuthorization jooqAuth = new JooqAccountAuthorization(f.metaDataManager);
        JooqResourceListSupport jooqList = new JooqResourceListSupport(f.jooqConfig, f.objectManager, f.metaDataManager);
        this.support = new DefaultResourceManagerSupport(objSupport, jooqList, jooqAuth);

        ApiRouterImpl routerImpl = new ApiRouterImpl(f.coreSchemaFactory);
        router = routerImpl;
        locator = routerImpl;

        Aes256Encrypter aes256Encrypter = new Aes256Encrypter();
        aes256Encrypter.init();
        PasswordHasher passwordHasher = new PasswordHasher();
        Sha256Hasher sha256Hasher = new Sha256Hasher();
        transformationService.addTransformers(aes256Encrypter);
        transformationService.addTransformers(passwordHasher);
        transformationService.addTransformers(sha256Hasher);
    }

}
