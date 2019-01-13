package com.bizlers.pigeons.agent.core;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import com.bizlers.pigeons.agent.utils.SystemInformation;
import com.bizlers.utils.ssfbuilder.SSLSocketFactoryBuilder;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

public class MqttConnector {
	private MqttClient mqttClient;
	private MqttConnectOptions options;
	private MessageListener messageListener;
	private int port = 0;
	String clientId = null;
	private AgentService agentService;

	public static final int KEEP_ALIVE_INTERVAL = 60 * 30; // seconds
	public static final int CONNECTION_TIMEOUT = 30; // seconds
	public static final long MQTT_OPERATION_TIMEOUT = 50 * 1000; // milliseconds
	private static final int RETRY_INTERVAL = 10000;

	public static final String MQTT_CONFIG_BIND_ADDRESS = "bind_address";
	public static final String MQTT_CONFIG_PORT = "port";
	public static final String MQTT_CONFIG_CONNECTION = "connection";
	public static final String MQTT_CONFIG_ADDRESS = "address";
	public static final String MQTT_CONFIG_TOPIC = "topic";
	public static final String MQTT_CONFIG_PASSWORD_FILE = "password_file";
	public static final String MQTT_CONFIG_BRIDGE_CA_CERTIFICATE_FILE = "bridge_cafile";
	public static final String MQTT_CONFIG_BRIDGE_CERTIFICATE_FILE = "bridge_certfile";
	public static final String MQTT_CONFIG_BRIDGE_KEYFILE = "bridge_keyfile";
	public static final String MQTT_USERNAME = "username";
	public static final String MQTT_PASSWORD = "password";
	public static final String MQTT_BRIDGE_TLS_VERSION = "bridge_tls_version";
	public static final String MQTT_BRIDGE_USERNAME = "bizlers";
	public static final String MQTT_BRIDGE_PASSWORD = "bizlers";
	public static final String MQTT_PERSISTENCE = "persistence";
	public static final String MQTT_PERSISTENCE_LOCATION = "persistence_location";
	public static final String MQTT_PERSISTENCE_FILE = "persistence_file";
	public static final String TOPIC_PIGEON_STATUS = "pigeons/pigeon/status";
	public static final String TOPIC_ADD_PIGEON = "pigeons/pigeon/add";
	public static final String TOPIC_PING = "pigeons/pigeon/ping";
	public static final String TOPIC_DESTROY_PIGEON = "pigeons/pigeon/destroy";
	public static final String TOPIC_SIGNAL = "pigeons/signal";
	public static final String TOPIC_PIGEON_MIRROR = "pigeons/pigeon/mirror/";

	public static final String STATUS_CONNECTED = "CONNECTED";
	public static final String STATUS_DISCONNECTED = "DISCONNECTED";
	public static final String STATUS_DISCONNECTED_UNEXPECTEDLY = "DISCONNECTED UNEXPECTEDLY";
	public static final String STATUS_MIRROR_PIGEON_REQUIRED = "MIRROR_PIGEON_REQUIRED";
	public static final int QOS_HIGH = 2;
	public Object reconnectLock = new Object();
	public boolean reconnecting = false;

	@Loggable(value = Loggable.DEBUG)
	public MqttConnector(String ipAddress, int port, String clientId,
			AgentMonitor agentMonitor, AgentService agentService)
			throws MqttException, PigeonAgentException, IOException,
			GeneralSecurityException {
		this.port = port;
		this.clientId = clientId;
		this.agentService = agentService;
		mqttClient = new MqttClient("ssl://" + ipAddress + ":" + port,
				clientId, null);
		messageListener = new MessageListener(clientId, agentMonitor,
				agentService);
		mqttClient.setCallback(messageListener);
		connect();
	}

	@Loggable(value = Loggable.DEBUG)
	private synchronized void connect() throws PigeonAgentException,
			IOException, GeneralSecurityException, MqttSecurityException,
			MqttException {
		if (mqttClient.isConnected()) {
			Logger.info(this, "SLB already connected.");
			return;
		}
		options = new MqttConnectOptions();
		options.setUserName(MQTT_BRIDGE_USERNAME);
		options.setPassword(MQTT_BRIDGE_PASSWORD.toCharArray());
		options.setCleanSession(true);
		options.setConnectionTimeout(CONNECTION_TIMEOUT);
		options.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
		AgentConfigurator agentConfigurator = agentService.getAgentConfigurator();
		SSLSocketFactory sslSocketFactory = new SSLSocketFactoryBuilder()
				.caCertificateFile(agentConfigurator.getCaCertificateFile())
				.clientCertificateFile(agentConfigurator.getClientCertificateFile())
				.keyFile(agentConfigurator.getKeyFile())
				.password(agentConfigurator.getBrokerPassword())
				.tlsVersion(agentConfigurator.getTlsVersion()).build();
		options.setSocketFactory(sslSocketFactory);
		MqttTopic topic = mqttClient.getTopic(TOPIC_PIGEON_STATUS);
		String string = clientId + ": STATUS_DISCONNECTED_UNEXPECTEDLY:" + port;
		byte[] bytes = new byte[255];
		bytes = string.getBytes();

		options.setWill(topic, bytes, 2, false);

		mqttClient.setTimeToWait(MQTT_OPERATION_TIMEOUT);
		mqttClient.connect(options);
	}

	@Loggable(value = Loggable.DEBUG)
	public void reconnect() {
		synchronized (reconnectLock) {
			if (reconnecting) {
				return;
			}
			reconnecting = true;
		}
		if (SystemInformation.getProcessId(port) != 0) {
			if (mqttClient != null && !mqttClient.isConnected()) {
				while (!mqttClient.isConnected()) {
					try {
						Thread.sleep(RETRY_INTERVAL);
					} catch (InterruptedException e1) {
					}
					try {
						connect();
						Logger.info(this, "Successfully Reconnected: %s",
								clientId);
					} catch (PigeonAgentException | IOException
							| GeneralSecurityException | MqttException e) {
						Logger.warn(this,
								"Failed to reconnect SLB. %[exception]s", e);
					}
				}
			}
		} else {
			Logger.info(
					this,
					"Broker at port %d is not up and working cannot reconnect.",
					port);
		}
		reconnecting = false;

	}

	@Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.SECONDS)
	public void disconnect() {
		Logger.info(this, "Disconnecting the Agent SLB");
		try {
			mqttClient.disconnect();
		} catch (MqttException e) {
			Logger.warn(this,
					"Exception occured while disconecting %[exception]s", e);
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.SECONDS)
	public void subscribe(String topicName) {
		Logger.info(this, "Subscribing to topic: '%s'", topicName);
		try {
			if (mqttClient.isConnected()) {
				mqttClient.subscribe(topicName);
			} else {
				Logger.warn(this, "Cannot subscribe as client is disconnected.");
			}
		} catch (MqttSecurityException e) {
			Logger.warn(this,
					"Exception occured while subscribing %[exception]s", e);
		} catch (MqttException e) {
			Logger.warn(this,
					"Exception occured while subscribing %[exception]s", e);
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.SECONDS)
	public boolean isConnected() {
		return mqttClient.isConnected();
	}

	@Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.SECONDS)
	public void unSubscribe(String topicName) {
		Logger.info(this, "unSubscribing to topic: '%s'", topicName);
		try {
			if (mqttClient.isConnected()) {
				mqttClient.unsubscribe(topicName);
			} else {
				Logger.warn(this,
						"Cannot unsubscribe as client is disconnected.");
			}
		} catch (MqttSecurityException e) {
			Logger.warn(this,
					"Exception occured while unsubscribing %[exception]s", e);
		} catch (MqttException e) {
			Logger.warn(this,
					"Exception occured while unsubscribing %[exception]s", e);
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.SECONDS)
	public void publish(String topicName, String message) {
		MqttMessage mqttMessage = null;
		MqttTopic topic = null;
		Logger.info(this, "Publishing to topic: '%s' message: %s", topicName,
				message);
		try {
			topic = mqttClient.getTopic(topicName);
			mqttMessage = new MqttMessage(message.getBytes());
			mqttMessage.setQos(QOS_HIGH);
			mqttMessage.setRetained(false);
			topic.publish(mqttMessage);
		} catch (MqttSecurityException e) {
			Logger.warn(this,
					"Exception occured while publishing %[exception]s", e);
		} catch (MqttException e) {
			Logger.warn(this,
					"Exception occured while publishing %[exception]s", e);
		}
	}

}
