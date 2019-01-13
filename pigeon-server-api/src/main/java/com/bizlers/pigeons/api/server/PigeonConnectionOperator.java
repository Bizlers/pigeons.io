package com.bizlers.pigeons.api.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.bizlers.pigeons.api.server.internal.MemoryMessageStore;
import com.bizlers.pigeons.api.server.internal.MemoryPigeonStore;
import com.bizlers.pigeons.api.server.internal.MessageStore;
import com.bizlers.pigeons.api.server.internal.PigeonStore;
import com.bizlers.pigeons.api.server.persistent.dao.DBMessageStore;
import com.bizlers.pigeons.api.server.persistent.dao.DBPigeonStore;
import com.bizlers.pigeons.api.server.persistent.models.MessageInfo;
import com.bizlers.pigeons.api.server.persistent.models.PigeonInfo;
import com.bizlers.pigeons.api.server.persistent.models.PigeonV2;
import com.bizlers.utils.jaxrs.SecuredClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class PigeonConnectionOperator {

	private static final int DEFAULT_ACK_TIME_OUT = 120; // In seconds.
	
	private static final long MQTT_OPERATION_TIMEOUT = 30 * 1000;

	private static final long PUBLISH_TIMEOUT = 10 * 1000;

	private static final int CONNECTION_TIMEOUT = 100;

	private static final int KEEP_ALIVE_INTERVAL = 10;

	private static final String STATUS_CONNECTED = "CONNECTED";

	private static final String STATUS_DISCONNECTED = "DISCONNECTED";

	private static final String STATUS_DISCONNECTED_UNEXPECTEDLY = "DISCONNECTED UNEXPECTEDLY";

	private static final String TOPIC_USER_CONNECT = "CONNECT/";

	private static final String TOPIC_USER_DISCONNECT = "DISCONNECT/";

	private static final String TOPIC_USER_ALLOCATE = "ALLOCATE/";

	private static final String TOPIC_USER_CLIENTID = "CLIENTID/";

	private static final String TOPIC_USER_ACK = "USER/ACK/";

	private static final String TOPIC_SYS_ACK_CONNECT = "ACK/CONNECT";

	private static final String TOPIC_SYS_ACK_DISCONNECT = "ACK/DISCONNECT";

	private static final String TOPIC_SYS_ACK_ALLOCATE = "ACK/ALLOCATE";

	private static final String TOPIC_PIGEON_STATUS = "pigeons/pigeon/status";

	private static final String URL_PIGEONS = "pigeons-server/rest/pigeons";

	private static final int ONLINE = 1;

	private static final int OFFLINE = 0;

	public static final int QOS_HIGH = 2;

	public static final boolean PERSISTENT = true;

	public static final boolean NON_PERSISTENT = false;

	private List<String> reconnectingClients = new ArrayList<String>();

	private Object reconnectLock = new Object();

	private Map<String, PigeonV2> pigeonMap = new HashMap<String, PigeonV2>();

	private ExecutorService arrivedMessageProcessor = Executors.newCachedThreadPool();
	
	private AcknowlegmentScheduler acknowlegmentScheduler;

	private PigeonServerUserConfig config;

	private long appId;

	private PigeonStore pigeonStore;

	private MessageStore messageStore;
	
	private Map<Integer, PigeonListener> listenersMap = new ConcurrentHashMap<>();

	public PigeonConnectionOperator(long appId, PigeonServerUserConfig config) {
		this.appId = appId;
		this.config = config;
		if (config.isMemoryStorage()) {
			pigeonStore = new MemoryPigeonStore();
			messageStore = new MemoryMessageStore();
		} else {
			pigeonStore = new DBPigeonStore();
			messageStore = new DBMessageStore();
		}
		
		int timeout = config.getAcknowlegmentTimeout();
		if (timeout == 0) {
			timeout = DEFAULT_ACK_TIME_OUT;
		}
		this.acknowlegmentScheduler = new AcknowlegmentScheduler(timeout);
	}

	public void setPigeonListener(PigeonListener pigeonListener) {
		listenersMap.put(pigeonListener.id, pigeonListener);
	}

	MqttClient createConnection(PigeonV2 pigeon) {
		pigeonMap.put(pigeon.getClientId(), pigeon);
		return createAndConnect(pigeon);
	}
	
	MqttClient createConnection(String clientId) {
		PigeonV2 pigeon = pigeonMap.get(clientId);
		return createAndConnect(pigeon);
	}
	
	private MqttClient createAndConnect(PigeonV2 pigeon) {
		Logger.trace(this, "Creating connection.");
		MqttClient mqttClient = null;
		try {
			mqttClient = buildFromPigeon(pigeon);
			connect(mqttClient, pigeon);
		} catch (PigeonServerException e) {
			String clientId = pigeon.getClientId();
			Logger.error(this, "Failed to connect - %s, ClientId: %s.", e.getMessage(), clientId);
			replacePigeon(clientId, mqttClient);
		}

		if (mqttClient.isConnected()) {
			return mqttClient;
		}
		return null;
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	private synchronized MqttClient connect(MqttClient mqttClient, PigeonV2 pigeon) throws PigeonServerException {
		if (mqttClient == null) {
			Logger.error(this, "Failed to connect - MqttClient is null");
			return null;
		}

		if (mqttClient.isConnected()) {
			Logger.error(this, "Client already connected. ClientId:" + mqttClient.getClientId());
			return mqttClient;
		}

		Logger.info(this, "Connecting to broker. URI: " + mqttClient.getServerURI());

		MqttConnectOptions options = getMqttConnectOptions(mqttClient, pigeon);
		
		try {
			mqttClient.connect(options);
		} catch (MqttException e) {
			Logger.error(this, "Connection Failed. ClientId: %s, Reason code: %d. Cause %s", pigeon.getClientId(),
					e.getReasonCode(), e.getCause());
			throw new PigeonServerException("Mqtt connection Failed.", e);
		}

		if (mqttClient.isConnected()) {
			Logger.info(this, "Connected to broker. ClientId: %s", pigeon.getClientId());
			getInitialSubscription(mqttClient, pigeon);
		} else {
			String errorMsg = "Client not connected. ClientId: " + pigeon.getClientId();
			Logger.error(this, errorMsg);
			throw new PigeonServerException(errorMsg, PigeonServerException.REASON_CODE_UNEXPECTED_ERROR);
		}
		return mqttClient;
	}

	private MqttConnectOptions getMqttConnectOptions(MqttClient mqttClient, PigeonV2 pigeon) throws PigeonServerException {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setConnectionTimeout(CONNECTION_TIMEOUT);
		options.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
		options.setPassword(pigeon.getPassword().toCharArray());
		options.setUserName(pigeon.getUserName());
		options.setSocketFactory(BrokerSSLConnector.getSocketFactory(config.getBrokerKeystorePath(),
				config.getBrokerKeystorePassword(), config.getTlsVersion()));
		options.setWill(mqttClient.getTopic(TOPIC_PIGEON_STATUS),
				(pigeon.getClientId() + ":" + STATUS_DISCONNECTED_UNEXPECTEDLY + ":" + pigeon.getPort()).getBytes(),
				QOS_HIGH, NON_PERSISTENT);
		return options;
	}

	private void getInitialSubscription(MqttClient mqttClient, PigeonV2 pigeon) throws PigeonServerException {
		try {
			mqttClient.subscribe(TOPIC_USER_CONNECT + appId);
			mqttClient.subscribe(TOPIC_USER_DISCONNECT + appId);
			mqttClient.subscribe(TOPIC_USER_ALLOCATE + appId);
			mqttClient.subscribe(TOPIC_USER_CLIENTID + appId);
			mqttClient.subscribe(TOPIC_USER_ACK + mqttClient.getClientId() + "/" + appId);
			mqttClient.getTopic(TOPIC_PIGEON_STATUS)
					.publish((pigeon.getClientId() + ":" + STATUS_CONNECTED + ":" + pigeon.getPort()).getBytes(),
							QOS_HIGH, NON_PERSISTENT)
					.waitForCompletion(PUBLISH_TIMEOUT);
		} catch (MqttException e) {
			Logger.error(this, "Initial publish/subscribe failed. ClientId: %s, Reason code: %d", pigeon.getClientId(),
					e.getReasonCode());
			throw new PigeonServerException("Initial publish/subscribe failed. " + e.getMessage(), e);
		}
		Logger.debug(this, "Initial subscription successfull. ClientId: %s", pigeon.getClientId());
	}

	private MqttClient buildFromPigeon(PigeonV2 pigeon) throws PigeonServerException {
		MqttClient mqttClient = null;
		try {
			mqttClient = new MqttClient("ssl://" + pigeon.getIp() + ":" + pigeon.getPort(), pigeon.getClientId(), null);
			mqttClient.setTimeToWait(MQTT_OPERATION_TIMEOUT);
			mqttClient.setCallback((new MqttListener(mqttClient)));
		} catch (MqttException e) {
			Logger.error(this, "Failed to create MqttClient. ClientId: %s. %[exception]s", pigeon.getClientId(), e);
			throw new PigeonServerException("Failed to create MqttClient - " + e.getMessage(), e);
		}
		Logger.trace(this, "Mqtt Client Created. ClientId: " + pigeon.getClientId());
		return mqttClient;
	}

	void reconnectMqttClient(MqttClient mqttClient) {
		try {
			connect(mqttClient, pigeonMap.get(mqttClient.getClientId()));
		} catch (PigeonServerException e) {
			Logger.warn(this, "Failed to reconnect - %s. ClientId: %s", e.getMessage(), mqttClient.getClientId());
		}
		if (!mqttClient.isConnected()) {
			replacePigeon(mqttClient.getClientId(), mqttClient);
		}
	}

	public void terminateConnection(MqttClient mqttClient) {
		acknowlegmentScheduler.terminateScheduler();
		if (mqttClient != null && mqttClient.isConnected()) {
			disconnect(mqttClient);
		}
	}

	// Replace the mqttClient's pigeon with new one and reconnect.
	private void replacePigeon(String clientId, MqttClient mqttClient) {
		PigeonV2 newPigeon = null;
		try {
			PigeonV2 pigeon = pigeonMap.get(clientId);
			newPigeon = getPigeonByClientId(pigeon.getClientId(), true);
			if (newPigeon != null) {
				pigeonMap.remove(clientId);
				pigeonMap.put(newPigeon.getClientId(), newPigeon);
				mqttClient.close();
				mqttClient = buildFromPigeon(newPigeon);
				connect(mqttClient, newPigeon);
			}
		} catch (PigeonServerException e) {
			Logger.warn(this, "Failed to replace pigeon - %s, Reason code: %d", e.getMessage(), e.getReasonCode());
		} catch (MqttException e) {
			Logger.warn(this, "Failed to replace pigeon - %s, Reason code: %d", e.getMessage(), e.getReasonCode());
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	private PigeonV2 getPigeonByClientId(String clientId, boolean replace) {
		PigeonV2 pigeon = null;
		ClientResponse response = null;
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		clientId = clientId.replaceAll("/", ":");
		try {
			Client client = new SecuredClientBuilder().keyStorePath(config.getKeyStorePath())
					.trustStorePath(config.getTrustStorePath()).keyStorePassword(config.getKeyStorePassword())
					.trustStorePassword(config.getTrustStorePassword())
					.privateKeyPassword(config.getPrivateKeyPassword()).build();
			client.addFilter(new HTTPBasicAuthFilter(String.valueOf(config.getUserId()), config.getSessionId()));

			WebResource webResource = null;
			if (replace) {
				webResource = client.resource("https://" + config.getPigeonServerHost() + ":"
						+ config.getPigeonServerPort() + "/" + URL_PIGEONS);
				queryParams.add("replace", String.valueOf(replace));
				queryParams.add("appId", String.valueOf(appId));
				queryParams.add("clientId", clientId);
			} else {
				webResource = client.resource("https://" + config.getPigeonServerHost() + ":"
						+ config.getPigeonServerPort() + "/" + URL_PIGEONS + "/" + String.valueOf(clientId));
			}
			response = webResource.queryParams(queryParams).get(ClientResponse.class);
			if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
				Logger.info(this, "Replace" + replace + "Failed to retrieved the pigeon -  " + ". HTTP error code : "
						+ response.getStatus(), replace);
			} else {
				pigeon = response.getEntity(PigeonV2.class);
				Logger.info(this, "Replace: %s Sucessfully retrieved the pigeon ", replace);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pigeon;
	}

	private void disconnect(MqttClient mqttClient) {
		Logger.trace(this, "Disconnecting");
		if (mqttClient != null && mqttClient.isConnected()) {
			try {
				String[] messagepart = mqttClient.getServerURI().split(":");
				String port = messagepart[2];
				byte[] payload = (mqttClient.getClientId() + ":" + STATUS_DISCONNECTED + ":" + port).getBytes();
				mqttClient.getTopic(TOPIC_PIGEON_STATUS).publish(payload, QOS_HIGH, NON_PERSISTENT)
						.waitForCompletion(PUBLISH_TIMEOUT);
				unSubsribeAll(mqttClient);
				mqttClient.disconnect();
			} catch (MqttException e) {
				Logger.warn(this, "Failed to disconnect publish pigeon. ClientId: " + mqttClient.getClientId());
			}
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	private void unSubsribeAll(MqttClient mqttClient) {
		Logger.trace(this, "Unsubscribing pigeon topics");
		try {
			mqttClient.unsubscribe(TOPIC_USER_CONNECT + appId);
			mqttClient.unsubscribe(TOPIC_USER_DISCONNECT + appId);
			mqttClient.unsubscribe(TOPIC_USER_ALLOCATE + appId);
			mqttClient.unsubscribe(TOPIC_USER_CLIENTID + appId);
			mqttClient.unsubscribe(TOPIC_USER_ACK + mqttClient.getClientId() + "/" + appId);
		} catch (MqttSecurityException e) {
			Logger.error(this, "Exception %[exception]s", e);
		} catch (MqttException e) {
			Logger.error(this, "Exception %[exception]s", e);
		}
	}

	private class MqttListener implements MqttCallback {

		private MqttClient mqttClient;

		public MqttListener(MqttClient mqttClient) {
			this.mqttClient = mqttClient;
		}

		@Override
		@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
		public void connectionLost(Throwable cause) {
			synchronized (reconnectLock) {
				if (reconnectingClients.contains(mqttClient.getClientId())) {
					Logger.debug(this, "Connection Lost - " + cause.getMessage()
							+ "(Connection already in progress). ClientId: " + mqttClient.getClientId());
					return;
				}
				reconnectingClients.add(mqttClient.getClientId());
			}
			Logger.warn(this, "Connection Lost - " + cause.getMessage() + ". Trying to reconnect. ClientId: "
					+ mqttClient.getClientId());
			reconnectMqttClient(mqttClient);
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
		public void messageArrived(String topic, MqttMessage message) {
			Logger.trace(this, "MqttMessage: " + message);
			arrivedMessageProcessor.execute(new ArrivedMessageProcessTask(topic, message.toString(), mqttClient));
		}
	}

	class ArrivedMessageProcessTask implements Runnable {

		private String topic;

		private String message;

		private MqttClient mqttClient;

		public ArrivedMessageProcessTask(String topic, String message, MqttClient mqttClient) {
			this.topic = topic;
			this.message = message;
			this.mqttClient = mqttClient;
		}
		
		@Override
		@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
		public void run() {
			processArrivedMessage(mqttClient, topic, message, mqttClient.getClientId());
		}

		private void processConnectMessage(long pigeonId) {
			Logger.info(this, "Connect message Arrived PairId: %d", pigeonId);
			PigeonInfo pigeonInfo = pigeonStore.get(pigeonId);
			if (pigeonInfo != null) {
				if (pigeonInfo.getStatus() != ONLINE) {
					pigeonInfo.setStatus(ONLINE);
					pigeonStore.update(pigeonInfo);
					for(PigeonListener pigeonListener:listenersMap.values()) {
						pigeonListener.clientConnected(pigeonId);
					}
				} else {
					Logger.debug(this, "Connect message ignored (Already online): %d", pigeonId);
				}
			} else {
				Logger.warn(this, "Connect message can not be processed (pigeonInfo is null): %d", pigeonId);
			}
		}

		private void processAllocateMessage(long pigeonId) {
			Logger.info(this, "Allocate message Arrived pigeonId: %d", pigeonId);
			PigeonInfo pigeonInfo = pigeonStore.get(pigeonId);
			if (pigeonInfo != null) {
				if (pigeonInfo.getStatus() != OFFLINE) {
					pigeonInfo.setStatus(OFFLINE);
					pigeonStore.update(pigeonInfo);
					for(PigeonListener pigeonListener:listenersMap.values()) {
						pigeonListener.clientDisconnected(pigeonId, PigeonV2.STATUS_ALLOTED);
					}
				} else {
					Logger.debug(this, "Allocate message ignored (Already offline): %d", pigeonId);
				}
			} else {
				Logger.warn(this, "Allocate message can not be processed (pigeonInfo is null): %d", pigeonId);
			}

		}

		private void processDisconnectMessage(long pigeonId) {
			Logger.info(this, "Disconnect message Arrived pigeonId: %d", pigeonId);

			PigeonInfo pigeonInfo = pigeonStore.get(pigeonId);
			if (pigeonInfo != null) {
				pigeonStore.delete(pigeonInfo.getPigeonId());
				for(PigeonListener pigeonListener:listenersMap.values()) {
					pigeonListener.clientDisconnected(pigeonId, PigeonV2.STATUS_DELETED);
				}
			} else {
				Logger.warn(this, "Disconnect message can not be processed (PigeonInfo is null): %d", pigeonId);
			}
		}

		private void processAcknowlegementMessage(Message pigeonMessage) {
			long messageId = pigeonMessage.getMessageId();
			Logger.debug(this, "MessageId = %d", messageId);
			MessageInfo msgInfo = messageStore.get(messageId);
			if (msgInfo != null) {
				PigeonListener pigeonListener = listenersMap.get(msgInfo.getPigeonListenerId());
				messageStore.delete(messageId);
				if (pigeonListener != null) {
					pigeonListener.messageDelivered(messageId, pigeonMessage.getPairId());
				}
			} else {
				Logger.warn(this, "MessageInfo for messageId %d does not exists.", messageId);
			}
		}

		private void processArrivedMessage(MqttClient client, String topic, String message, String clientId) {
			Logger.trace(this, "Message arrived topic: %s, message: %s, clientId: %s", topic, message, clientId);
			try {
				if (topic.equalsIgnoreCase(TOPIC_USER_ACK + client.getClientId() + "/" + appId)) {
					Message pigeonMessage = new ObjectMapper().readValue(message, Message.class);
					processAcknowlegementMessage(pigeonMessage);
				} else if (topic.equalsIgnoreCase(TOPIC_USER_CONNECT + appId)) {
					long pigeonId = Long.parseLong(message);
					client.publish(TOPIC_SYS_ACK_CONNECT, String.valueOf(pigeonId).getBytes(), QOS_HIGH, NON_PERSISTENT);
					processConnectMessage(pigeonId);
				} else if (topic.equalsIgnoreCase(TOPIC_USER_ALLOCATE + appId)) {
					long pigeonId = Long.parseLong(message);
					client.publish(TOPIC_SYS_ACK_ALLOCATE, String.valueOf(pigeonId).getBytes(), QOS_HIGH, NON_PERSISTENT);
					processAllocateMessage(pigeonId);
				} else if (topic.equalsIgnoreCase(TOPIC_USER_DISCONNECT + appId)) {
					long pigeonId = Long.parseLong(message);
					client.publish(TOPIC_SYS_ACK_DISCONNECT, String.valueOf(pigeonId).getBytes(), QOS_HIGH, NON_PERSISTENT);
					processDisconnectMessage(pigeonId);
				} else {
					Logger.warn(this, "Invalid Topic %s .", topic);
				}
			} catch (Exception e) {
				Logger.warn(this, "Exception occured in processArrivedMessage %[exception]s", e);
			}
		}
	}
	
	private class AcknowlegmentScheduler {

		private Timer timer;

		private int seconds;

		AcknowlegmentScheduler(int seconds) {
			timer = new Timer();
			this.seconds = seconds;
			timer.schedule(new RemindTask(), seconds * 1000, seconds * 1000);
		}

		void terminateScheduler() {
			timer.cancel();
		}

		class RemindTask extends TimerTask {
			@Loggable(value = Loggable.INFO, limit = 10, unit = TimeUnit.SECONDS)
			public void run() {
				try {
					long currentTime = System.currentTimeMillis();
					List<MessageInfo> messages = messageStore.get(currentTime, seconds * 1000);
					if (messages != null && !messages.isEmpty()) {
						for (MessageInfo message : messages) {
							PigeonListener pigeonListener = listenersMap.get(message.getPigeonListenerId());
							if(pigeonListener != null) {
								pigeonListener.messageLost(message.getMessageId(), message.getPigeonId());
							}
						}
						messageStore.delete(currentTime, seconds * 1000);
					}
				} catch (Exception e) {
					Logger.warn(this, "Exception in RemindTask  %[exception]s", e);
				}
			}
		}
	}
	
	public MessageStore getMessageStore() {
		return messageStore;
	}
	
	public PigeonStore getPigeonStore() {
		return pigeonStore;
	}
}
