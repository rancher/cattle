package io.github.ibuildthecloud.dstack.docker.client;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringProperty;

public class DockerClient {

    private static final Logger log = LoggerFactory.getLogger(DockerClient.class);

    private static final String TOKEN_HEADER = "X-Docker-Token";
    private static final String REPO = "x-docker-endpoints";
    private static final String TOKEN = "x-docker-token";

    private static final DynamicStringProperty INDEX_URL = ArchaiusUtil.getString("docker.index.url");
    private static final DynamicStringProperty INDEX_USER = ArchaiusUtil.getString("docker.index.user");
    private static final DynamicStringProperty INDEX_PASS = ArchaiusUtil.getString("docker.index.pass");


    Executor executor;
    JsonMapper jsonMapper;

    public DockerImage lookup(final DockerImage image) throws IOException {
        String url = String.format("%s/v1/repositories/%s/images", INDEX_URL.get(), image.getQualifiedName());

        Request r = Request.Get(url)
            .addHeader(TOKEN_HEADER, "true");

        return executor.execute(r).handleResponse(new ResponseHandler<DockerImage>() {
            @Override
            public DockerImage handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if ( response.getStatusLine().getStatusCode() != 200 ) {
                    return null;
                }

                Header endpoints = response.getFirstHeader(REPO);
                Header token = response.getFirstHeader(TOKEN);

                if ( endpoints == null || token == null ) {
                    throw new IOException("Did not get both " + REPO + " and " + TOKEN + " in response");
                }

                return getImage(image, endpoints.getValue().split("\\s*,\\s*")[0], token.getValue());
            }
        });
    }

    protected DockerImage getImage(final DockerImage image, String endpoint, String token) throws IOException {
        String url = String.format("https://%s/v1/repositories/%s/tags", endpoint, image.getQualifiedName());
        Request r = Request.Get(url).addHeader("Authorization", "Token " + token);

        return executor.execute(r).handleResponse(new ResponseHandler<DockerImage>() {
            @Override
            public DockerImage handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if ( response.getStatusLine().getStatusCode() != 200 ) {
                    return null;
                }

                Map<String,Object> data = jsonMapper.readValue(response.getEntity().getContent());

                Object id = data.get(image.getTag());
                if ( id == null ) {
                    return null;
                } else {
                    image.setId(id.toString());
                    return image;
                }
            }
        });
    }

    @PostConstruct
    public void init() {
        Runnable onChange = new Runnable() {
            @Override
            public void run() {
                init();
            }
        };

        INDEX_URL.addCallback(onChange);
        INDEX_USER.addCallback(onChange);
        INDEX_PASS.addCallback(onChange);

        log.info("Using docker index url [{}] and user [{}]", INDEX_URL.get(), INDEX_USER.get());

        Executor executor = Executor.newInstance();
        if ( ! StringUtils.isBlank(INDEX_USER.get()) ) {
            executor = executor.auth(INDEX_USER.get(), INDEX_PASS.get());
        }

        this.executor = executor;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }
}
