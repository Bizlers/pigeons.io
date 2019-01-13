package com.bizlers.pigeons.agent.core;

import java.io.File;
import java.net.URL;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

import com.bizlers.pigeons.commommodels.Broker;
import com.jcabi.log.Logger;

public class AgentDaemon implements Daemon {

	private AgentService agentService;
	
	public static void main(String args[]) {
		String configPath = null;
		if (args[0] != null && !args[0].isEmpty()) {
			configPath = args[0];
		} else {
			URL configUrl = ClassLoader.getSystemResource("agent.config");
			configPath = configUrl.getPath();
		}

		if (configPath != null) {
			AgentDaemon agentDaemon = new AgentDaemon();
			try {
				agentDaemon.init(configPath);
				agentDaemon.start();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			Runtime.getRuntime().addShutdownHook(new Thread() {

				@Override
				public void run() {
					try {
						agentDaemon.stop();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						agentDaemon.destroy();
					}
				}
			});
		} else {
			Logger.error(AgentDaemon.class, "Agent configuration file missing.");
		}
	}

	@Override
	public void init(DaemonContext context) throws DaemonInitException, Exception {
		File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
		String configPath = jarFile.getParent() + File.separator + "agent.config";
		if(new File(configPath).exists()) {
			init(configPath );
		} else {
			Logger.error(this, "Agent configuration file missing. Searched at %s", configPath);
			context.getController().fail("Agent configuration file missing.");
		}
	}
	
	private void init(String configPath) throws Exception {
		agentService = new AgentService();
		agentService.initialise(configPath);
	}

	@Override
	public void start() throws Exception {
		agentService.startService();
		agentService.checkAllBrokers();
		AgentMonitor.INSTANCE.startMonitor(agentService.getBrokerStatusListener(), agentService);
		for (Broker broker : agentService.getBridgedBrokers()) {
			new BridgeVerifier(agentService, broker).verify();
		}
		agentService.generatePigeons();
	}

	@Override
	public void stop() throws Exception {
		Logger.info(this, "Shutting down the agent...");
		AgentMonitor.INSTANCE.stopMonitor();
		if(agentService != null) {
			agentService.stopService();
		}
		Logger.info(this, "Agent has been terminated succesfully. ");
	}

	@Override
	public void destroy() {
		agentService = null;
	}
}
