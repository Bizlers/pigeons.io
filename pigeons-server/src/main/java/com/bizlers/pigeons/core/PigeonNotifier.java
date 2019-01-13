package com.bizlers.pigeons.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

import com.bizlers.pigeons.core.MqttConnectionPool.MqttConnectionOperator;
import com.bizlers.pigeons.models.ConnectionMessage;
import com.bizlers.pigeons.models.Pigeon;
import com.bizlers.pigeons.utils.BrokerSSLConnector;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

@Component
@Scope("singleton")
public class PigeonNotifier implements MqttConnectionOperator {

	private static final long PIGEON_APPID = 0;
	private static final int QOS = 2;

	private static final String SCHEME_SSL = "ssl://";

	private static final String TOPIC_USER_CONNECT = "CONNECT/";
	private static final String TOPIC_USER_DISCONNECT = "DISCONNECT/";
	private static final String TOPIC_USER_ALLOCATE = "ALLOCATE/";

	private static final String TOPIC_SYS_ACK = "ACK/#";
	private static final String TOPIC_SYS_ACK_CONNECT = "ACK/CONNECT";
	private static final String TOPIC_SYS_ACK_DISCONNECT = "ACK/DISCONNECT";
	private static final String TOPIC_SYS_ACK_ALLOCATE = "ACK/ALLOCATE";

	private static final int KEEP_ALIVE_INTERVAL = 10000;
	private static final long PUBLISH_TIMEOUT = 10 * 1000;
	private static final int ACK_TIME_OUT = 120 * 1000;
	private static final long MQTT_OPERATION_TIMEOUT = 50 * 1000;
	private static final int CONNECTION_TIMEOUT = 100;

	private static boolean RETAINED = false;

	private MqttConnectionPool connectionPool;

	private Map<String, Pigeon> pigeonMap;

	private BlockingQueue<ConnectionMessage> connectionMessages;

	@Autowired
	private PigeonOperator pigeonOperator;

	private LostMessagePublisher lostMessagePublisher;

	private ExecutorService publishExecutor;

	private List<String> reconnectingClients;

	private Object reconnectLock = new Object();

	@Loggable(value = Loggable.DEBUG)
	private PigeonNotifier() {
		pigeonMap = new HashMap<String, Pigeon>();
		reconnectingClients = new ArrayList<String>();
		connectionMessages = new DelayQueue<ConnectionMessage>();
		lostMessagePublisher = new LostMessagePublisher();
		publishExecutor = Executors.newFixedThreadPool(12);
	}

	@Override
	public MqttClient createConnection(String clientId) {
		MqttClient mqttClient = null;
		Pigeon pigeon = pigeonMap.get(clientId);
		try {
			mqttClient = getMqttClient(clientId, pigeon);
			connect(mqttClient, pigeon);
		} catch (PigeonServerException e) {
			Logger.error(this, "Failed to connect - %s, ClientId: %s.",
					e.getMessage(), clientId);
			replacePigeon(clientId, mqttClient);
		}

		if (mqttClient.isConnected()) {
			return mqttClient;
		}
		return null;
	}

	@Override
	public void reconnectMqttClient(MqttClient mqttClient) {
		reconnect(mqttClient.getClientId(), mqttClient);
		if (!mqttClient.isConnected()) {
			replacePigeon(mqttClient.getClientId(), mqttClient);
		}
	}

	@Override
	public void terminateConnection(MqttClient mqttClient) {
		if (mqttClient != null && mqttClient.isConnected()) {
			disconnect(mqttClient);
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public synchronized void connect() {
		if (isConnected()) {
			Logger.info(this, "INFO:Already connected.");
			return;
		}

		List<Pigeon> pigeonList = pigeonOperator
				.getPublishPigeonList(PIGEON_APPID);
		if (pigeonList == null) {
			String errorMessage = "Publish pigeons not found.";
			Logger.error(this, errorMessage);
			return;
		}

		Logger.info(this, "Publish pigeons retrived. Count: %d",
				pigeonList.size());
		for (Pigeon pigeon : pigeonList) {
			pigeonMap.put(pigeon.getClientId(), pigeon);
		}

		connectionPool = new MqttConnectionPool(pigeonList, this);
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	private synchronized MqttClient connect(MqttClient mqttClient, Pigeon pigeon)
			throws PigeonServerException {
		if (mqttClient == null) {
			Logger.error(this, "Failed to connect - MqttClient is null");
			return null;
		}

		if (mqttClient.isConnected()) {
			Logger.error(this, "Client already connected. ClientId:"
					+ mqttClient.getClientId());
			return mqttClient;
		}

		Logger.info(this,
				"Connecting to broker. URI: " + mqttClient.getServerURI());

		MqttConnectOptions options = getMqttConnectOptions(pigeon);
		try {
			mqttClient.connect(options);
			mqttClient.subscribe(TOPIC_SYS_ACK);
			pigeon.setStatus(Pigeon.STATUS_ONLINE);
			pigeonOperator.updatePigeonStatus(pigeon);
		} catch (MqttException e) {
			Logger.error(
					this,
					"MqttException: Connection Failed. ClientId: %s, Reason code: %d",
					pigeon.getClientId(), e.getReasonCode());
			throw new PigeonServerException("Mqtt connection Failed.", e);
		}
		return mqttClient;
	}

	private void reconnect(String clientId, MqttClient mqttClient) {
		try {
			connect(mqttClient, pigeonMap.get(clientId));
		} catch (PigeonServerException e) {
			Logger.warn(this, "Failed to reconnect - %s. ClientId: %s",
					e.getMessage(), mqttClient.getClientId());
		}
	}

	// Replace the mqttClient's pigeon with new one and reconnect.
	private void replacePigeon(String clientId, MqttClient mqttClient) {
		Pigeon newPigeon = null;
		try {
			newPigeon = pigeonOperator.getPublishPigeonByBroker(PIGEON_APPID,
					pigeonOperator.readPigeon(mqttClient.getClientId())
							.getBrokerName());
			if (newPigeon != null) {
				pigeonMap.remove(clientId);
				pigeonMap.put(newPigeon.getClientId(), newPigeon);

				mqttClient.close();
				pigeonOperator.deletePigeon(clientId);

				mqttClient = getMqttClient(newPigeon.getClientId(), newPigeon);
				connect(mqttClient, newPigeon);
			}
		} catch (MqttException e) {
			Logger.warn(this, "Failed to replace pigeon - %s, Reason code: %d",
					e.getMessage(), e.getReasonCode());
		} catch (PigeonServerException e) {
			Logger.warn(this, "Failed to replace pigeon - %s", e.getMessage());
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public void disconnect() throws PigeonServerException {
		if (connectionPool != null) {
			connectionPool.shutdown();
			if (connectionPool.isConnected()) {
				Logger.error(this, "Failed to disconnect.");
				throw new PigeonServerException("Failed to disconnect.");
			}
		}
	}

	private void disconnect(MqttClient mqttClient) {
		if (mqttClient != null && mqttClient.isConnected()) {
			try {
				mqttClient.disconnect();
			} catch (MqttException e) {
				Logger.warn(this,
						"Failed to disconnect publish pigeon. ClientId: "
								+ mqttClient.getClientId());
			}
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public boolean isConnected() {
		if (connectionPool != null) {
			return connectionPool.isConnected();
		}
		return false;
	}

	@Loggable(value = Loggable.DEBUG)
	public void terminate() {
		try {
			lostMessagePublisher.terminateScheduler();
			publishExecutor.shutdown();
			disconnect();
		} catch (PigeonServerException e) {
			Logger.warn(this, "Failed to terminate pigeon notifier.");
		}
	}

	private MqttClient getMqttClient(String clientId, Pigeon pigeon)
			throws PigeonServerException {
		MqttClient mqttClient = null;
		try {
			mqttClient = new MqttClient(SCHEME_SSL + pigeon.getIp() + ":"
					+ pigeon.getPort(), pigeon.getClientId(), null);
			mqttClient.setTimeToWait(MQTT_OPERATION_TIMEOUT);
			mqttClient.setCallback(new MqttListener(clientId, mqttClient));
		} catch (MqttException e) {
			Logger.error(this,
					"Failed to create MqttClient. ClientId: %s. %[exception]s",
					pigeon.getClientId(), e);
			throw new PigeonServerException("Failed to create MqttClient - "
					+ e.getMessage(), e);
		}
		Logger.trace(this,
				"Mqtt Client Created. ClientId: " + pigeon.getClientId());
		return mqttClient;
	}

	private MqttConnectOptions getMqttConnectOptions(Pigeon pigeon)
			throws PigeonServerException {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setConnectionTimeout(CONNECTION_TIMEOUT);
		options.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
		options.setPassword(pigeon.getPassword().toCharArray());
		options.setUserName(pigeon.getUserName());
		options.setSocketFactory(BrokerSSLConnector.getSocketFactory());
		return options;
	}

	@Loggable(value = Loggable.DEBUG)
	public void publish(Pigeon pigeon, char messageType) {
		connect();
		ConnectionMessage connectionMessage = new ConnectionMessage(
				pigeon.getAppId(), pigeon.getId(), messageType);
		publishExecutor.execute(new PublishTask(connectionMessage));
	}

	@Loggable(value = Loggable.DEBUG)
	private void sendMessage(MqttClient mqttClient, long appId, long pigeonId,
			char messageType) {
		try {
			if (mqttClient != null && mqttClient.isConnected()) {
				String topic = null;
				if (messageType == ConnectionMessage.CONNECT_MESSAGE) {
					topic = TOPIC_USER_CONNECT + appId;
				} else if (messageType == ConnectionMessage.ALLOCATE_MESSAGE) {
					topic = TOPIC_USER_ALLOCATE + appId;
				} else if (messageType == ConnectionMessage.DISCONNECT_MESSAGE) {
					topic = TOPIC_USER_DISCONNECT + appId;
				} else {
					Logger.info(this, "Invalid message type: " + messageType);
					return;
				}
				mqttClient
						.getTopic(topic)
						.publish(String.valueOf(pigeonId).getBytes(), QOS,
								RETAINED).waitForCompletion(PUBLISH_TIMEOUT);
			}
		} catch (MqttException e) {
			Logger.warn(this,
					"Failed to send message. Exception: %[exception]s", e);
		}
	}

	private class MqttListener implements MqttCallback {

		private String clientId;

		private MqttClient mqttClient;

		public MqttListener(String clientId, MqttClient mqttClient) {
			this.clientId = clientId;
			this.mqttClient = mqttClient;
		}

		@Override
		@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
		public void connectionLost(Throwable cause) {
			synchronized (reconnectLock) {
				if (reconnectingClients.contains(clientId)) {
					Logger.debug(
							this,
							"Connection Lost - "
									+ cause.getMessage()
									+ "(Connection already in progress). ClientId: "
									+ mqttClient.getClientId());
					return;
				}
				reconnectingClients.add(mqttClient.getClientId());
			}
			Logger.info(
					this,
					"Connection Lost - " + cause.getMessage()
							+ ". Trying to reconnect. ClientId: "
							+ mqttClient.getClientId());
			reconnectMqttClient(mqttClient);
			reconnectingClients.remove(mqttClient.getClientId());
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
		public void messageArrived(String topic, MqttMessage message) {
			if (topic.equalsIgnoreCase(TOPIC_SYS_ACK_CONNECT)) {
				long pigeonId = Long.parseLong(message.toString());
				connectionMessages.remove(new ConnectionMessage(pigeonId,
						ConnectionMessage.CONNECT_MESSAGE));
			} else if (topic.equalsIgnoreCase(TOPIC_SYS_ACK_ALLOCATE)) {
				long pigeonId = Long.parseLong(message.toString());
				connectionMessages.remove(new ConnectionMessage(pigeonId,
						ConnectionMessage.ALLOCATE_MESSAGE));
			} else if (topic.equalsIgnoreCase(TOPIC_SYS_ACK_DISCONNECT)) {
				long pigeonId = Long.parseLong(message.toString());
				connectionMessages.remove(new ConnectionMessage(pigeonId,
						ConnectionMessage.DISCONNECT_MESSAGE));
			} else {
				Logger.info(this,
						"Acknowledgement received with invalid topic. Topic: "
								+ topic);
			}
		}
	}

	private class PublishTask implements Runnable {

		private ConnectionMessage connectionMessage;

		@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
		public PublishTask(ConnectionMessage connectionMessage) {
			this.connectionMessage = connectionMessage;
		}

		@Override
		@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
		public void run() {
			MqttClient mqttClient = null;
			try {
				connectionMessage.setSentTime(System.currentTimeMillis());
				connectionMessages.add(connectionMessage);
				mqttClient = connectionPool.borrowObject();
				if (mqttClient != null) {
					sendMessage(mqttClient, connectionMessage.getAppId(), connectionMessage.getPigeonId(),
							connectionMessage.getMessageType());
					Logger.debug(this, "Sent - " + connectionMessage);
				} else {
					Logger.warn(this, "mqttClient from connectionPool is null.");
				}
			} finally {
				connectionPool.returnObject(mqttClient);
			}
		}
	}

	private class LostMessagePublisher {

		private Timer timer;

		public LostMessagePublisher() {
			timer = new Timer();
			timer.schedule(new ConnectionMessageTimer(), ACK_TIME_OUT,
					ACK_TIME_OUT);
		}

		public void terminateScheduler() {
			timer.cancel();
		}

		class ConnectionMessageTimer extends TimerTask {
			@Loggable(value = Loggable.INFO, limit = 10, unit = TimeUnit.SECONDS)
			public void run() {
				try {
					ConnectionMessage message = null;
					while ((message = connectionMessages.poll()) != null) {
						Logger.info(this, "Resending connection message. "
								+ message);
						publishExecutor.execute(new PublishTask(message));
					}
				} catch (Exception e) {
					Logger.warn(
							this,
							"Exception occured while sending lost connection messages. %[exception]s",
							e);
				}
			}
		}
	}
}