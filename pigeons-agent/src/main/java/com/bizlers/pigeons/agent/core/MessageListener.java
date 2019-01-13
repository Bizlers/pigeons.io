package com.bizlers.pigeons.agent.core;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

import expectj.ExpectJException;
import expectj.TimeoutException;

public class MessageListener implements MqttCallback {

	private static final int SIGNAL_RELOAD = 1;
	private String clientId;
	private OnConnectionLostListener onConnectionLostListener;
	private AgentService agentService;
	private ExecutorService executorService = Executors.newCachedThreadPool();

	public MessageListener(String clientId,
			OnConnectionLostListener onConnectionLostListener,
			AgentService agentService) {
		this.clientId = clientId;
		this.onConnectionLostListener = onConnectionLostListener;
		this.agentService = agentService;
	}

	@Override
	@Loggable(value = Loggable.TRACE)
	public void connectionLost(Throwable cause) {
		Logger.warn(this,
				"Connection Lost: Client Id: %s Cause: %[exception]s",
				clientId, cause);
		onConnectionLostListener.onConnectionLost();
	}

	private class ProcessMessage implements Runnable {
		private String topic;
		private MqttMessage message;

		ProcessMessage(String topic, MqttMessage message) {
			this.topic = topic;
			this.message = message;
		}

		public void run() {
			Logger.trace(this, " ClientId : %s  Topic : %s Message %s", clientId, topic, message.toString());
			if ((topic.toString()).equals(MqttConnector.TOPIC_PIGEON_STATUS)) {
				updateStatus(message);
			} else if ((topic.toString()).equals(MqttConnector.TOPIC_PING)) {
				processPing(message);
			} else if ((topic.toString()).equals(MqttConnector.TOPIC_ADD_PIGEON)) {
				addNewPigeon(message);
			} else if ((topic.toString()).equals(MqttConnector.TOPIC_DESTROY_PIGEON)) {
				destroyPigeon(message);
			} else if ((topic.toString()).equals(MqttConnector.TOPIC_SIGNAL)) {
				handleSignal(message);
			} else {
				Logger.info(this, " Invalid Topic : %s Message %s", topic, message.toString());
			}
		}
	}

	@Override
	@Loggable(value = Loggable.TRACE)
	public void messageArrived(String topic, MqttMessage message) {
		executorService.execute(new ProcessMessage(topic, message));
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		Logger.info(this, "Delivery Complete");

	}

	@Loggable(value = Loggable.DEBUG)
	private void handleSignal(MqttMessage message) {
		// Message format [signal:port:username:password:action]
		try {
			String[] splitMessage = message.toString().split(":");
			int signal = Integer.parseInt(splitMessage[0]);
			int port = Integer.parseInt(splitMessage[1]);
			String userName = splitMessage[2];
			String password = splitMessage[3];
			String action = splitMessage[4];
			if (signal == SIGNAL_RELOAD) {
				agentService.reloadConfiguration(port, action, userName,
						password);
			}
		} catch (NumberFormatException | PigeonAgentException e) {
			Logger.warn(this, "Exception in handleSignal %[exception]s", e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	private void addNewPigeon(MqttMessage message) {
		// Message format [port:clientId:username:password]
		String msg[] = new String[4];
		msg = message.toString().split(":", 4);
		int port = Integer.parseInt(msg[0]);
		String clientId = msg[1];
		String userName = msg[2];
		String password = msg[3];
		try {
			agentService.addNewPigeon(clientId, port, userName, password);
		} catch (IOException | ExpectJException | TimeoutException
				| NumberFormatException e) {
			Logger.warn(this, "Adding new Pigeon Exception %[exception]s", e);
		}
	}

	private void destroyPigeon(MqttMessage message) {
		// Message format [port:clientId:username]
		String msg[] = new String[3];
		msg = message.toString().split(":", 3);
		int port = Integer.parseInt(msg[0]);
		String clientId = msg[1];
		String userName = msg[2];
		try {
			agentService.destroyPigeon(clientId, port, userName);
		} catch (NumberFormatException e) {
			Logger.warn(this, "Exception while deleting pigeon %[exception]s",
					e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	private void processPing(MqttMessage message) {
		// Message format [port:clientId:clientApiVersion]
		String msg[] = new String[3];
		msg = message.toString().split(":", 3);
		int port = Integer.parseInt(msg[0]);
		String clientId = msg[1];
		int clientApiVersion = Integer.parseInt(msg[2]);
		agentService.processPing(port, clientId, clientApiVersion);
	}

	@Loggable(value = Loggable.DEBUG)
	private void updateStatus(MqttMessage message) {
		// Message format
		// Disconnect [clientId:status:port]
		// Connect [clientId:status:port:pigeonClientAPIVersion]
		String msg[] = new String[4];
		msg = message.toString().split(":", 4);
		String clientId = msg[0];
		String status = msg[1];
		int port = Integer.parseInt(msg[2]);

		if (agentService.getAgentConfigurator().isRedirectPortAvailable()
				&& agentService.getAgentConfigurator().getRedirectPort() == port) {
			port = agentService.getAgentConfigurator()
					.getRedirectDestinationPort();
		}

		try {
			if (status.equals(MqttConnector.STATUS_CONNECTED)) {
				// Old pigeon client API (till version 0) won't provide API
				// version. So if not provided consider it as 0.
				int clientApiVersion = (msg.length == 4) ? Integer
						.parseInt(msg[3]) : 0;
				agentService
						.onClientConnected(clientId, port, clientApiVersion);
			} else if (status.equals(MqttConnector.STATUS_DISCONNECTED)
					|| status
							.equals(MqttConnector.STATUS_DISCONNECTED_UNEXPECTEDLY)) {
				agentService.onClientDisconnect(clientId, port);
			}
		} catch (PigeonAgentException | NumberFormatException e) {
			Logger.warn(this, "Exception in updateStatus %[exception]s", e);
		}
	}

	public interface OnConnectionLostListener {
		public void onConnectionLost();
	}
}
