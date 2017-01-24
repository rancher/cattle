package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class NetworkPolicyRule {

    public enum NetworkPolicyRuleAction {
        allow, deny
    }

    public enum NetworkPolicyRuleWithin {
        stack, service, linked
    }

    NetworkPolicyRuleMember from, to;
    NetworkPolicyRuleBetween between;
    NetworkPolicyRuleAction action;
    NetworkPolicyRuleWithin within;
    String[] ports;

    public String[] getPorts() {
        return ports;
    }

    public void setPorts(String[] ports) {
        this.ports = ports;
    }

    public NetworkPolicyRuleAction getAction() {
        return action;
    }

    public void setAction(NetworkPolicyRuleAction action) {
        this.action = action;
    }

    public NetworkPolicyRuleWithin getWithin() {
        return within;
    }

    public void setWithin(NetworkPolicyRuleWithin within) {
        this.within = within;
    }

    public NetworkPolicyRuleMember getFrom() {
        return from;
    }

    public void setFrom(NetworkPolicyRuleMember from) {
        this.from = from;
    }

    public NetworkPolicyRuleMember getTo() {
        return to;
    }

    public void setTo(NetworkPolicyRuleMember to) {
        this.to = to;
    }

    public NetworkPolicyRuleBetween getBetween() {
        return between;
    }

    public void setBetween(NetworkPolicyRuleBetween between) {
        this.between = between;
    }
}
