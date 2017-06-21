package io.cattle.platform.app;

import io.cattle.platform.api.schema.builder.SchemaFactoryBuilder;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.resource.ResourceLoader;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class SchemasConfig {


    @DependsOn("CoreSchemaFactory")
    SchemaFactory SuperadminSchema(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory, JsonMapper jsonMapper) throws IOException {
        return SchemaFactoryBuilder.id("superadmin")
                .parent(coreSchemaFactory)
                .jsonAuthOverlay(jsonMapper, "schema/super-admin/super-admin-auth.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory AdminSchema(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("admin")
                .parent(coreSchemaFactory)
                .jsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/admin")
                .jsonAuthOverlay(jsonMapper,
                        "schema/user/user-auth.json",
                        "schema/admin/admin-auth.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory ServiceSchema(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("service")
                .parent(coreSchemaFactory)
                .jsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/service")
                .jsonAuthOverlay(jsonMapper,
                        "schema/user/user-auth.json",
                        "schema/admin/admin-auth.json",
                        "schema/project/project-auth.json",
                        "schema/service/service-auth.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory Token(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("token")
                .parent(coreSchemaFactory)
                .jsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/token")
                .jsonAuthOverlay(jsonMapper,
                        "schema/token/token-auth.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory ReadOnlySchema(@Qualifier("Project") SchemaFactory projectSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("readonly")
                .parent(projectSchemaFactory)
                .notWriteable()
                .jsonAuthOverlay(jsonMapper,
                        "schema/read-user/read-user.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory OwnerSchema(@Qualifier("Project") SchemaFactory projectSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("owner")
                .parent(projectSchemaFactory)
                .jsonAuthOverlay(jsonMapper,
                        "schema/owner/owner-auth.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory MemberSchema(@Qualifier("Project") SchemaFactory projectSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("member")
                .parent(projectSchemaFactory)
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory Restricted(@Qualifier("Project") SchemaFactory projectSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("restricted")
                .parent(projectSchemaFactory)
                .jsonAuthOverlay(jsonMapper, "schema/restricted-user/restricted-user.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory ReadAdminSchema(@Qualifier("AdminSchema") SchemaFactory adminSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("readAdmin")
                .parent(adminSchemaFactory)
                .notWriteable()
                .jsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/read-admin")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory User(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("user")
                .parent(coreSchemaFactory)
                .jsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/user")
                .jsonAuthOverlay(jsonMapper, "schema/user/user-auth.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory Project(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("project")
                .parent(coreSchemaFactory)
                .jsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/project")
                .jsonAuthOverlay(jsonMapper,
                        "schema/user/user-auth.json",
                        "schema/project/project-auth.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory Environment(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("environment")
                .parent(coreSchemaFactory)
                .jsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/environment")
                .jsonAuthOverlay(jsonMapper,
                        "schema/user/user-auth.json",
                        "schema/project/project-auth.json",
                        "schema/environment/environment-auth.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory ProjectAdmin(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("projectadmin")
                .parent(coreSchemaFactory)
                .jsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/projectadmin")
                .jsonAuthOverlay(jsonMapper,
                        "schema/user/user-auth.json",
                        "schema/project/project-auth.json",
                        "schema/projectadmin/projectadmin-auth.json")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory AgentRegister(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("agentRegister")
                .parent(coreSchemaFactory)
                .notWriteable()
                .whitelistJsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/agent-register")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory AgentSchema(@Qualifier("CoreSchemaFactory") SchemaFactory coreSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("agent")
                .parent(coreSchemaFactory)
                .notWriteable()
                .whitelistJsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/agent")
                .build();
    }

    @DependsOn("CoreSchemaFactory")
    SchemaFactory RegisterSchema(@Qualifier("AdminSchema") SchemaFactory adminSchemaFactory, JsonMapper jsonMapper,
            io.github.ibuildthecloud.gdapi.json.JsonMapper schemasMarshaller, ResourceLoader resourceLoader) throws IOException {
        return SchemaFactoryBuilder.id("register")
                .parent(adminSchemaFactory)
                .jsonSchemaFromPath(jsonMapper, schemasMarshaller, resourceLoader, "schema/register")
                .jsonAuthOverlay(jsonMapper,
                        "schema/register/register-auth.json")
                .build();
    }

}