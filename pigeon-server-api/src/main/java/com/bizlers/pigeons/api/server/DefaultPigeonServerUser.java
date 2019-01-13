package com.bizlers.pigeons.api.server;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import com.bizlers.pigeons.api.server.internal.ServerRegistrationAgent;
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

public class DefaultPigeonServerUser implements PigeonServerUser {

	private static final int OFFLINE = 0;

	private static final long PUBLISH_TIMEOUT = 10 * 1000;

	private static final String TOPIC_PRIFIX_USER_BROADCAST = "USER/BROADCAST/";

	private static final String TOPIC_PREFIX_USER = "USER/";

	private static final String URL_PIGEONS = "pigeons-server/rest/pigeons";

	private static final int QOS_HIGH = 2;

	private static final boolean NON_PERSISTENT = false;

	private long appId;

	private static long messageCount = 0;

	private static long publishMessageCount = 0;

	private Client client;

	private PigeonListener pigeonListener;

	private ExecutorService arrivedMessageProcessor;

	private ExecutorService publishMessagesExecutor;

	private PublishPigeonPool connectionPool;

	private PigeonServerUserConfig config;

	private static final ObjectMapper mapper = new ObjectMapper();

	public static DefaultPigeonServerUser fromConfig(PigeonServerUserConfig config) throws PigeonServerException {
		DefaultPigeonServerUser pigeonServerUser = new DefaultPigeonServerUser(config);
		try {
			Client client = new SecuredClientBuilder().keyStorePath(config.getKeyStorePath())
					.trustStorePath(config.getTrustStorePath()).keyStorePassword(config.getKeyStorePassword())
					.trustStorePassword(config.getTrustStorePassword())
					.privateKeyPassword(config.getPrivateKeyPassword()).build();
			client.addFilter(new HTTPBasicAuthFilter(String.valueOf(config.getUserId()), config.getSessionId()));
			pigeonServerUser.client = client;
			pigeonServerUser.appId = ServerRegistrationAgent.INSTANCE.register(config).getAppId();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pigeonServerUser;
	}

	private DefaultPigeonServerUser(PigeonServerUserConfig config) {
		this.config = config;
	}

	public void setPigeonListener(PigeonListener pigeonListener) {
		this.pigeonListener = pigeonListener;
	}

	@Override
	public void connect(boolean waitForConnection) throws PigeonServerException {
		if (!waitForConnection) {
			connect();
		} else {
			int count = 0;
			while (count < 10) {
				try {
					Logger.info(this, "Trying to connect Pigeons system. Attempt (%d / 10)", count);
					connect();
					if (!isConnected()) {
						Logger.info(this, "Retrying after 5 seconds");
						Thread.sleep(5000);
					} else {
						Logger.info(this, "Successfully connected to Pigeons system.");
						break;
					}
				} catch (PigeonServerException e) {
					Logger.error(this, "Exception: %s", e.getMessage());
				} catch (InterruptedException e) {
					Logger.error(this, "Exception %[exception]s", e);
				}
				count++;
			}
		}
	}

	@Override
	public void connect() throws PigeonServerException {
		if (!isConnected()) {
			connectionPool = PublishPigeonPool.load(config);
			connectionPool.setPigeonListener(pigeonListener);
			if (connectionPool.getSize() > 0) {
				publishMessagesExecutor = Executors.newCachedThreadPool();
				arrivedMessageProcessor = Executors.newCachedThreadPool();
			} else {
				Logger.error(this, "mqttClientList is Empty");
			}
		} else {
			Logger.info(this, "Already connected.");
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public void disconnect() throws PigeonServerException {
		connectionPool.shutdown();
		arrivedMessageProcessor.shutdown();
		if (connectionPool.isConnected()) {
			Logger.error(this, "Failed to disconnect.");
			throw new PigeonServerException("Failed to disconnect.");
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public boolean isConnected() {
		return (connectionPool != null && connectionPool.isConnected());
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public PigeonV2 getPigeon() throws PigeonServerException {
		return getPigeon("", 0);
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public PigeonV2 getPigeon(int port) throws PigeonServerException {
		return getPigeon("", port);
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public PigeonV2 getPigeon(String region, int port) throws PigeonServerException {
		PigeonV2 pigeon = null;
		if (client != null) {
			WebResource webResource = client.resource(
					"https://" + config.getPigeonServerHost() + ":" + config.getPigeonServerPort() + "/" + URL_PIGEONS);
			MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
			queryParams.add("region", region);
			queryParams.add("port", String.valueOf(port));
			queryParams.add("appId", String.valueOf(appId));
			ClientResponse response = webResource.queryParams(queryParams).get(ClientResponse.class);
			if (response.getClientResponseStatus() == ClientResponse.Status.OK) {
				pigeon = response.getEntity(PigeonV2.class);
				PigeonInfo pigeonInfo = new PigeonInfo(pigeon.getId(), pigeon.getClientId(), OFFLINE);
				connectionPool.getPigeonConnectionOperator().getPigeonStore().insert(pigeonInfo);
			} else if (response.getClientResponseStatus() == ClientResponse.Status.NOT_FOUND) {
				Logger.warn(this, "Pigeon for %s region and %d port not found. Check pigeons-server has pigeons ready.",
						region, port);
			} else {
				Logger.error(this, "HTTP error code: %d", response.getStatus());
				throw new PigeonServerException(
						"Failed to retrieved the pigeon - " + ".HTTP error code: " + response.getStatus());
			}
		} else {
			throw new IllegalStateException(
					"Not connected to pigeons server. Please ensure publish pigeons are connected.");
		}
		return pigeon;
	}

	private class PublishTask implements Runnable {

		private PublishItem publishItem;

		@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
		PublishTask(PublishItem publishItem) {
			this.publishItem = publishItem;
		}

		@Override
		@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
		public void run() {
			MqttClient connection = null;
			try {
				connection = connectionPool.borrowObject();
				if (connection != null) {
					if (publishItem.isMultiCast()) {
						publish(publishItem.getTopic(), publishItem.getMessage(), publishItem.getQos(),
								publishItem.isRetained(), connection);
					} else {
						sendMessage(publishItem.getMessageId(), publishItem.getPigeon(), publishItem.getMessage(),
								publishItem.isAcknowledgement(), connection);
					}
				}
			} catch (PigeonServerException e) {
				Logger.error(this, "Exception  %[exception]s", e);
			} finally {
				if (connection != null) {
					connectionPool.returnObject(connection);
				}
			}
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public long sendMessageWithoutAck(long pigeonId, String message) throws PigeonServerException {
		return sendMessage(pigeonId, message, false);
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	public long sendMessage(long pigeonId, String message) throws PigeonServerException {
		return sendMessage(pigeonId, message, true);
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS)
	private long sendMessage(long pigeonId, String message, boolean acknowlegment) throws PigeonServerException {
		Logger.debug(this, "sendMessage(%d, %s, %s)", pigeonId, message, acknowlegment);
		PigeonInfo pigeon = connectionPool.getPigeonConnectionOperator().getPigeonStore().get(pigeonId);
		if (pigeon != null) {
			long messageId = getMessageId();
			publishMessagesExecutor
					.execute(new PublishTask(new PublishItem(messageId, pigeon, message, acknowlegment, false)));
			return messageId;
		} else {
			String errorMessage = "Failed to send message. PigenoId is Invalid or disconnected.";
			Logger.error(this, errorMessage);
			throw new PigeonServerException(errorMessage, PigeonServerException.REASON_CODE_INVALID_PIGEON_ID);
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.MILLISECONDS)
	private synchronized long getMessageId() {
		if (messageCount == Long.MAX_VALUE) {
			messageCount = 0;
		}
		return ++messageCount;
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.MILLISECONDS)
	private synchronized long getPublishMessageId() {
		if (publishMessageCount == Long.MAX_VALUE) {
			publishMessageCount = 0;
		}
		return ++publishMessageCount;
	}

	@Loggable(value = Loggable.DEBUG, limit = 5, unit = TimeUnit.SECONDS)
	private void sendMessage(long messageId, PigeonInfo pigeon, String message, boolean acknowledgement,
			MqttClient mqttClient) throws PigeonServerException {
		if (!mqttClient.isConnected()) {
			Logger.error(this, "Not connected to Server.");
			throw new PigeonServerException("Not connected to Server.");
		}
		if (pigeon.getClientId() == null || message == null) {
			Logger.error(this, "clientId or message cannot be null.");
			throw new PigeonServerException("clientId or message cannot be null.");
		}

		Message pigeonMessage = new Message(appId, messageId, message, acknowledgement, pigeon.getPigeonId());

		if (mqttClient.isConnected()) {
			try {
				pigeonMessage.setPublishBroker(mqttClient.getClientId());
				byte[] payload = mapper.writeValueAsString(pigeonMessage).getBytes();
				mqttClient.getTopic(TOPIC_PREFIX_USER + pigeon.getClientId()).publish(payload, QOS_HIGH, NON_PERSISTENT)
						.waitForCompletion(PUBLISH_TIMEOUT);
			} catch (MqttException | IOException e) {
				Logger.warn(this, "Exception while sending message.");
				throw new PigeonServerException("Exception while sending message.");
			}
		}

		if (acknowledgement) {
			connectionPool.getPigeonConnectionOperator().getMessageStore()
					.insert(new MessageInfo(messageId, System.currentTimeMillis(), pigeon.getPigeonId(), pigeonListener.id));
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.MILLISECONDS)
	public void publish(String topic, String message) throws PigeonServerException {
		publishMessagesExecutor
				.execute(new PublishTask(new PublishItem(topic, message, QOS_HIGH, NON_PERSISTENT, true)));
	}

	@Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.MILLISECONDS)
	private void publish(String topic, String message, int qos, boolean retained, MqttClient mqttClient)
			throws PigeonServerException {
		String errorMessage;
		if (!mqttClient.isConnected()) {
			Logger.error(this, "Not connected to Server.");
			throw new PigeonServerException("Not connected to Server.");
		}
		if (topic == null || message == null) {
			Logger.error(this, "Topic or message cannot be null.");
			throw new PigeonServerException("Topic or message cannot be null.");
		}

		byte[] payload;
		BroadcastMessage broadcastMessage = new BroadcastMessage(getPublishMessageId(), message);
		try {
			payload = mapper.writeValueAsString(broadcastMessage).getBytes();
		} catch (IOException e) {
			errorMessage = "Failed to publish.";
			Logger.error(this, errorMessage + "Exception  %[exception]s", e);
			throw new PigeonServerException(errorMessage, e);
		}

		if (mqttClient.isConnected()) {
			try {
				mqttClient.getTopic(TOPIC_PRIFIX_USER_BROADCAST + appId + "/" + topic).publish(payload, qos, retained)
						.waitForCompletion(PUBLISH_TIMEOUT);
			} catch (MqttException e) {
				errorMessage = "Failed to publish.";
				Logger.error(this, "Exception  %[exception]s", e);
				throw new PigeonServerException(errorMessage, e);
			}
		}
	}
}
