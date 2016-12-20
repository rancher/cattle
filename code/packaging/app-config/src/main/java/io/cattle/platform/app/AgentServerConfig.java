package io.cattle.platform.app;

import io.cattle.platform.agent.connection.simulator.AgentSimulator;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorConfigUpdateProcessor;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorConsoleProcessor;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorDelegateProcessor;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorFailedProcessor;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorInstanceInspectProcessor;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorPingProcessor;
import io.cattle.platform.agent.connection.simulator.impl.SimulatorStartStopProcessor;
import io.cattle.platform.agent.server.ping.dao.impl.PingDaoImpl;
import io.cattle.platform.agent.server.ping.impl.PingMonitorImpl;
import io.cattle.platform.agent.server.resource.impl.AgentResourcesMonitor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentServerConfig {

    @Bean
    AgentSimulator agentSimulator() {
        return new AgentSimulator();
    }

    @Bean
    SimulatorConfigUpdateProcessor simulatorConfigUpdateProcessor() {
        return new SimulatorConfigUpdateProcessor();
    }

    @Bean
    SimulatorConsoleProcessor simulatorConsoleProcessor() {
        return new SimulatorConsoleProcessor();
    }

    @Bean
    SimulatorDelegateProcessor simulatorDelegateProcessor() {
        return new SimulatorDelegateProcessor();
    }

    @Bean
    SimulatorFailedProcessor simulatorFailedProcessor() {
        return new SimulatorFailedProcessor();
    }

    @Bean
    SimulatorPingProcessor simulatorPingProcessor() {
        return new SimulatorPingProcessor();
    }

    @Bean
    SimulatorStartStopProcessor simulatorStartStopProcessor() {
        return new SimulatorStartStopProcessor();
    }

    @Bean
    SimulatorInstanceInspectProcessor simulatorInstanceInspectProcessor() {
        return new SimulatorInstanceInspectProcessor();
    }

    @Bean
    PingDaoImpl pingDaoImpl() {
        return new PingDaoImpl();
    }

    @Bean
    PingMonitorImpl pingMonitorImpl() {
        return new PingMonitorImpl();
    }

    @Bean
    AgentResourcesMonitor agentResourcesMonitor() {
        return new AgentResourcesMonitor();
    }
}
