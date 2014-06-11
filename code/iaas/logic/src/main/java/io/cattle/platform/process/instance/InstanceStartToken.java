package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.security.SecureRandom;

import javax.inject.Named;

import org.apache.commons.codec.binary.Base64;

@Named
public class InstanceStartToken extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        String token = instance.getToken();

        if ( token == null ) {
            byte[] buffer = new byte[64];
            RANDOM.nextBytes(buffer);
            token = Base64.encodeBase64String(buffer).replaceAll("[/=+]", "");
        }

        return new HandlerResult(INSTANCE.TOKEN, token);
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.start" };
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
