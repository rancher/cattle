package io.cattle.platform.core.addon;

import java.util.List;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class NetworkPolicySpec {

	DefaultPolicyAction defaultPolicyAction;
	List<NetworkPolicy> policies;	
	
	public enum DefaultPolicyAction {
        allow,
        deny
    }

	public NetworkPolicySpec(DefaultPolicyAction defaultPolicyAction, List<NetworkPolicy> policies) {
		super();
		this.defaultPolicyAction = defaultPolicyAction;
		this.policies = policies;
	}

	public DefaultPolicyAction getDefaultPolicyAction() {
		return defaultPolicyAction;
	}

	@Field(defaultValue = "allow")
	public void setDefaultPolicyAction(DefaultPolicyAction defaultPolicyAction) {
		this.defaultPolicyAction = defaultPolicyAction;
	}

	public List<NetworkPolicy> getPolicies() {
		return policies;
	}

	public void setPolicies(List<NetworkPolicy> policies) {
		this.policies = policies;
	}

}
