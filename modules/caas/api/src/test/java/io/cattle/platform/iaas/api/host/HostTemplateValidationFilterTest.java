package io.cattle.platform.iaas.api.host;

import io.cattle.platform.core.constants.HostTemplateConstants;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static io.cattle.platform.iaas.api.host.HostTemplateValidationFilter.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class HostTemplateValidationFilterTest {

    @Test
    public void validateDriver() throws Exception {
        HostTemplateValidationFilter htvf = new HostTemplateValidationFilter();

        Map<String, Object> data = new HashMap<>();
        data.put("driver", "digitalocean");
        assertEquals("digitalocean", htvf.validateStringField(data, "driver"));
    }

    @Test
    public void validateName() throws Exception {
        HostTemplateValidationFilter htvf = new HostTemplateValidationFilter();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "non-empty-test-name");
        assertEquals("non-empty-test-name", htvf.validateStringField(data, "name"));
    }

    @Test
    public void validateNameIllFormed() throws Exception {
        HostTemplateValidationFilter htvf = new HostTemplateValidationFilter();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "  ill-formed-test-name  ");

        try {
            htvf.validateStringField(data, "name");
            Assert.fail();
        } catch (ClientVisibleException e) {
            assertEquals(STRING_FIELD_ILL_FORMED, e.getMessage());
        }
    }

    @Test
    public void validateEmptyDriver() throws Exception {
        HostTemplateValidationFilter htvf = new HostTemplateValidationFilter();

        HashMap<String, Object> data = new HashMap<>();
        data.put("driver", "");

        try {
            htvf.validateStringField(data, "driver");
            Assert.fail();
        } catch (ClientVisibleException e) {
            assertEquals(STRING_FIELD_EMPTY, e.getMessage());
        }
    }

    @Test
    public void validateEmptyName() throws Exception {
        HostTemplateValidationFilter htvf = new HostTemplateValidationFilter();

        HashMap<String, Object> data = new HashMap<>();
        data.put("name", "");

        try {
            htvf.validateStringField(data, "name");
            Assert.fail();
        } catch (ClientVisibleException e) {
            assertEquals(STRING_FIELD_EMPTY, e.getMessage());
        }
    }

    @Test
    public void validateNoConfig() throws Exception {
        HostTemplateValidationFilter htvf = new HostTemplateValidationFilter();

        Map<String, Object> data = new HashMap<>();

        try {
            htvf.validateConfigKey(data, "digitalocean");
            Assert.fail();
        } catch (ClientVisibleException e) {
            assertEquals(DRIVER_CONFIG_EXACTLY_ONE_REQUIRED, e.getMessage());
        }
    }

    @Test
    public void createSingleDriverConfig() throws Exception {
        HostTemplateValidationFilter htvf = new HostTemplateValidationFilter();
        ApiRequest request = new ApiRequest(null, null);

        Map<String, Object> data = new HashMap<>();
        data.put("driver", "digitalocean");
        data.put("name", "test-do-ht1");
        Map<String, Object> values = new HashMap<>();
        values.put("digitaloceanConfig", new HashMap<>());
        data.put("secretValues", values);
        data.put("publicValues", values);

        assertEquals("digitaloceanConfig", htvf.validateConfigKey(data, "digitalocean"));

        request.setRequestObject(data);
        ResourceManager next = mock(ResourceManager.class);

        htvf.create(HostTemplateConstants.KIND, request, next);
        Mockito.verify(next).create(HostTemplateConstants.KIND, request);
    }

    @Test
    public void createWrongDriverConfig() throws Exception {
        HostTemplateValidationFilter htvf = new HostTemplateValidationFilter();
        ApiRequest request = new ApiRequest(null, null);

        Map<String, Object> data = new HashMap<>();
        data.put("driver", "amazonec2");
        data.put("name", "test-aws-ht1");
        Map<String, Object> values = new HashMap<>();
        values.put("digitaloceanConfig", new HashMap<>());
        data.put("secretValues", values);
        data.put("publicValues", values);

        request.setRequestObject(data);
        ResourceManager next = mock(ResourceManager.class);

        try {
            htvf.create(HostTemplateConstants.KIND, request, next);
            Assert.fail();
        } catch (ClientVisibleException e) {
            assertEquals(DRIVER_CONFIG_FOR_WRONG_DRIVER, e.getMessage());
        } finally {
            Mockito.verifyZeroInteractions(next);
        }
    }

    @Test
    public void createMultipleDriverConfig() throws Exception {
        HostTemplateValidationFilter htvf = new HostTemplateValidationFilter();

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> secretValues = new HashMap<>();
        secretValues.put("digitaloceanConfig", new HashMap<>());
        Map<String, Object> publicValues = new HashMap<>();
        publicValues.put("amazonec2Config", new HashMap<>());
        data.put("secretValues", secretValues);
        data.put("publicValues", publicValues);

        try {
            htvf.validateConfigKey(data, "digitalocean");
            Assert.fail();
        } catch (ClientVisibleException e) {
            assertEquals(DRIVER_CONFIG_EXACTLY_ONE_REQUIRED, e.getMessage());
        }
    }

}