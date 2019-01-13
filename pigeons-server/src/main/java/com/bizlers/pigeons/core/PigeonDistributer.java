package com.bizlers.pigeons.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bizlers.pigeons.models.Agent;
import com.bizlers.pigeons.models.Pigeon;
import com.bizlers.pigeons.utils.BrokerSSLConnector;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

/**
 * @author Prashant
 * 
 */
@Component
@Scope("singleton")
public class PigeonDistributer {
	private static final int KEEP_ALIVE_INTERVAL = 10000;
	private static final long PUBLISH_TIMEOUT = 10 * 1000;
	private static final long MQTT_OPERATION_TIMEOUT = 50 * 1000;

	private static final int QOS_HIGH = 2;
	private static final boolean RETAINED = false;
	private static final String SCHEME_SSL = "ssl://";

	private static final String TOPIC_SIGNAL = "pigeons/signal";
	private static final String TOPIC_ADD_PIGEON = "pigeons/pigeon/add";
	private static final String TOPIC_DESTROY_PIGEON = "pigeons/pigeon/destroy";

	private static final String SLB_USERNAME = "bizlers";
	private static final String SLB_PASSWORD = "bizlers";

	private static final String SIGNAL_RELOAD = "1";
	private static final String STATUS_ADD = "A";
	private static final String STATUS_DELETE = "D";

	private Map<Long, MqttClient> agentSLBConnection = new HashMap<Long, MqttClient>();

	@Autowired
	private PigeonOperator pigeonOperator;
	
	@Autowired
	private AgentOperator agentOperator;

	private MqttClient getMqttClient(Long agentId) {
		MqttClient client = null;
		Agent agent = agentOperator.readAgent(agentId);

		client = agentSLBConnection.get(agent.getAgentId());
		if (client == null || !client.isConnected()) {
			agentSLBConnection.remove(agent.getAgentId());
			connect(agent);
			client = agentSLBConnection.get(agent.getAgentId());
		}
		return client;
	}

	/*
	 * Make a connection to the status listener broker (SLB) of the provided
	 * agent.
	 */
	private void connect(Agent agent) {
		MqttClient client = null;
		try {
			MqttConnectOptions options = new MqttConnectOptions();
			options.setCleanSession(true);
			options.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
			options.setSocketFactory(BrokerSSLConnector.getSocketFactory());
			options.setUserName(SLB_USERNAME);
			options.setPassword(SLB_PASSWORD.toCharArray());

			client = new MqttClient(SCHEME_SSL + agent.getIp() + ":"
					+ agent.getStatusListenerBrokerPort(), agent.getAgentName()
					+ "-" + agent.getAgentId(), null);

			client.setTimeToWait(MQTT_OPERATION_TIMEOUT);
			client.setCallback(new MqttCallbackImpl(agent.getAgentId()));
			client.connect(options);

			if (client.isConnected()) {
				agentSLBConnection.put(agent.getAgentId(), client);
			}
		} catch (MqttException e) {
			Logger.error(this, "Failed to connect. %[exception]s", e);
		} catch (PigeonServerException e) {
			Logger.error(this, "Failed to connect. %[exception]s", e);
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 100, unit = TimeUnit.SECONDS)
	public void distributePigeons(long agentId) {
		Map<Long, List<Pigeon>> pigeonMap = pigeonOperator
				.getAgentPigeons(agentId);
		distributePigeons(pigeonMap);
	}

	@Loggable(value = Loggable.DEBUG, limit = 100, unit = TimeUnit.SECONDS)
	public void distributePigeons(String type) {
		Map<Long, List<Pigeon>> pigeonMap = pigeonOperator
				.getPigeonsReadyToDistribute(type);
		distributePigeons(pigeonMap);
	}

	@Loggable(value = Loggable.DEBUG, limit = 100, unit = TimeUnit.SECONDS)
	public void distributePigeons(Map<Long, List<Pigeon>> pigeonMap) {
		if (pigeonMap == null || pigeonMap.isEmpty()) {
			return;
		}

		Map<Long, Pigeon> reloadMap = new HashMap<Long, Pigeon>();

		for (Long agentId : pigeonMap.keySet()) {
			MqttClient client = getMqttClient(agentId);
			if (client != null) {
				Logger.info(this, "Distributing pigeons for agent: %d", agentId);
				List<Pigeon> pigeons = pigeonMap.get(agentId);
				for (Pigeon pigeon : pigeons) {
					reloadMap.put((long) pigeon.getPort(), pigeon);
					// Message format [port:clientId:username:password]
					String message = pigeon.getPort() + ":"
							+ pigeon.getClientId() + ":" + pigeon.getUserName()
							+ ":" + pigeon.getPassword();
					try {
						publish(client, TOPIC_ADD_PIGEON, message);
					} catch (MqttException e) {
						Logger.error(
								this,
								"Failed to distribute pigeon: %d, %[exception]s",
								pigeon.getId(), e);
					}
				}

				// Add message for all pigeons of current agent has been sent.
				// Now send reload signal for the associated brokers.
				Set<Long> portsToReload = reloadMap.keySet();
				Logger.debug(this, "Sending reload signal to ports: %s",
						portsToReload.toString());
				for (Long port : portsToReload) {
					try {
						publish(getMqttClient(reloadMap.get(port).getAgentId()),
								TOPIC_SIGNAL, SIGNAL_RELOAD + ":"
										+ reloadMap.get(port).getPort() + ":"
										+ reloadMap.get(port).getUserName()
										+ ":"
										+ reloadMap.get(port).getPassword()
										+ ":" + STATUS_ADD);
					} catch (MqttException e) {
						Logger.error(
								this,
								"Failed to send reload signal to port %d, %[exception]s",
								port, e);
					}
				}
			} else {
				Logger.warn(this, "MqttClient is null for agentId %d ", agentId);

			}
		}
	}

	/*
	 * Send message to agent associated with the provided pigeon to add the
	 * pigeon credentials to the password file and make it available for use.
	 */
	public void distributePigeon(Pigeon pigeon) {
		try {
			// Message format [port:clientId:username:password]
			String message = pigeon.getPort() + ":" + pigeon.getClientId()
					+ ":" + pigeon.getUserName() + ":" + pigeon.getPassword();

			MqttClient client = getMqttClient(pigeon.getAgentId());
			publish(client, TOPIC_ADD_PIGEON, message);
		} catch (MqttException e) {
			Logger.error(this,
					"Failed to distribute pigeon: %d, %[exception]s",
					pigeon.getId(), e);
		}
	}

	public void destroyPigeons(int idleTime) {
		Map<Long, List<Pigeon>> pigeonMap = pigeonOperator
				.getPigeonsReadyToDestroy(idleTime);
		if (pigeonMap == null || pigeonMap.isEmpty()) {
			return;
		}
		Map<Long, Pigeon> reloadMap = new HashMap<Long, Pigeon>();

		for (Long agentId : pigeonMap.keySet()) {
			MqttClient client = getMqttClient(agentId);
			if (client != null) {
				Logger.info(this, "Destroying pigeons for agent: %d", agentId);
				List<Pigeon> pigeons = pigeonMap.get(agentId);
				for (Pigeon pigeon : pigeons) {
					try {
						reloadMap.put((long) pigeon.getPort(), pigeon);

						// Message format [port:clientId:username]
						String message = pigeon.getPort() + ":"
								+ pigeon.getClientId() + ":"
								+ pigeon.getUserName();

						publish(client, TOPIC_DESTROY_PIGEON, message);
					} catch (MqttException e) {
						Logger.error(this,
								"Failed to destroy pigeon: %d, %[exception]s",
								pigeon.getId(), e);
					}
				}

				// Destroy message for all pigeons of current agent has been
				// sent. Now send reload signal for the associated brokers.
				Set<Long> portsToReload = reloadMap.keySet();
				Logger.debug(this, "Sending reload signal to ports: %s",
						portsToReload.toString());
				for (Long port : portsToReload) {
					try {
						publish(getMqttClient(reloadMap.get(port).getAgentId()),
								TOPIC_SIGNAL, SIGNAL_RELOAD + ":"
										+ reloadMap.get(port).getPort() + ":"
										+ reloadMap.get(port).getUserName()
										+ ":"
										+ reloadMap.get(port).getPassword()
										+ ":" + STATUS_DELETE);
					} catch (MqttException e) {
						Logger.error(
								this,
								"Failed to send reload signal to port %d, %[exception]s",
								port, e);
					}
				}
			} else {
				Logger.warn(this, "MqttClient is null for agentId %d ", agentId);
			}
		}
	}

	/*
	 * Send message to agent associated with the provided pigeon to destroy the
	 * provided pigeon.
	 */
	public void destroyPigeon(Pigeon pigeon) {
		try {
			// Message format [port:clientId:username]
			String message = pigeon.getPort() + ":" + pigeon.getClientId()
					+ ":" + pigeon.getUserName();

			MqttClient client = getMqttClient(pigeon.getAgentId());
			publish(client, TOPIC_DESTROY_PIGEON, message);
			publish(getMqttClient(pigeon.getAgentId()),
					TOPIC_SIGNAL,
					SIGNAL_RELOAD + ":" + pigeon.getPort() + ":"
							+ pigeon.getUserName() + ":" + pigeon.getPassword()
							+ ":" + STATUS_DELETE);
		} catch (MqttException e) {
			Logger.error(this, "Failed to destroy pigeon: %d, %[exception]s",
					pigeon.getId(), e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	private void publish(MqttClient mqttClient, String topic, String message)
			throws MqttException {
		mqttClient.getTopic(topic)
				.publish(message.getBytes(), QOS_HIGH, RETAINED)
				.waitForCompletion(PUBLISH_TIMEOUT);
	}

	private class MqttCallbackImpl implements MqttCallback {

		private long agentId;

		public MqttCallbackImpl(long agentId) {
			this.agentId = agentId;
		}

		@Override
		public void connectionLost(Throwable cause) {
			Logger.warn(PigeonDistributer.class,
					"SLB connection Lost. AgentId: %d, Cause: %s", agentId,
					cause.getMessage());
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
		}

		@Override
		public void messageArrived(String message, MqttMessage token)
				throws Exception {
		}
	}
}
