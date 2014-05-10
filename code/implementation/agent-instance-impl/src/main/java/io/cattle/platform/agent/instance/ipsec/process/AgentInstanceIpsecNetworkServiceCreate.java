package io.cattle.platform.agent.instance.ipsec.process;

import static io.cattle.platform.core.model.tables.DataTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.model.Data;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.security.SecureRandom;

import org.apache.commons.codec.binary.Hex;

import com.netflix.config.DynamicIntProperty;

public class AgentInstanceIpsecNetworkServiceCreate extends AbstractObjectProcessLogic implements ProcessPostListener {

    private static final DynamicIntProperty LENGTH = ArchaiusUtil.getInt("ipsec.psk.byte.length");

    SecureRandom random = new SecureRandom();

    @Override
    public String[] getProcessNames() {
        return new String[] { "networkservice.create" };
    }

    protected String randomKey() {
        byte[] bytes = new byte[LENGTH.get()];
        random.nextBytes(bytes);
        return Hex.encodeHexString(bytes);
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        NetworkService networkService = (NetworkService)state.getResource();

        if ( ! NetworkServiceConstants.KIND_IPSEC_TUNNEL.equals(networkService.getKind()) ) {
            return null;
        }

        NetworkServiceProvider provider = loadResource(NetworkServiceProvider.class, networkService.getNetworkServiceProviderId());

        if ( provider == null || ! provider.getKind().equals(NetworkServiceProviderConstants.KIND_AGENT_INSTANCE) ) {
            return null;
        }

        String key = String.format("%s/%s", networkService.getUuid(), "ipsecKey");
        Data data = objectManager.findAny(Data.class, DATA.NAME, key);

        if ( data == null ) {
            objectManager.create(Data.class,
                    DATA.NAME, key,
                    DATA.VALUE, randomKey(),
                    DATA.VISIBLE, false);
        }

        return null;
    }


}
