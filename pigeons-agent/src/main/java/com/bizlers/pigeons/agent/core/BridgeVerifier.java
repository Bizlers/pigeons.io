package com.bizlers.pigeons.agent.core;

import java.util.List;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.bizlers.pigeons.commommodels.Broker;
import com.bizlers.utils.ssfbuilder.SSLSocketFactoryBuilder;
import com.jcabi.log.Logger;

public class BridgeVerifier {

	private static final String MQTT_BRIDGE_USERNAME = "bizlers";

	private static final String MQTT_BRIDGE_PASSWORD = "bizlers";

	public static final int KEEP_ALIVE_INTERVAL = 60 * 30; // seconds

	public static final int CONNECTION_TIMEOUT = 30; // seconds

	public static final long MQTT_OPERATION_TIMEOUT = 50 * 1000; // milliseconds

	private AgentService agentService;

	private Broker broker;

	public BridgeVerifier(AgentService agentService, Broker source) {
		this.agentService = agentService;
		this.broker = source;
	}

	public void verify() {
		List<Broker> connectedBrokers = broker.getConnectedTo();
		if (connectedBrokers != null && !connectedBrokers.isEmpty()) {
			Logger.debug(this, "Verifying bridges for broker " + broker);
			Logger.debug(this, "Broker %s is connected to %s", broker, connectedBrokers);
			for (Broker broker : connectedBrokers) {
				verifyBridge(broker, this.broker);
			}
		} else {
			Logger.trace(this, "Broker at port has no bridge connections");
		}
	}

	private void verifyBridge(final Broker source, final Broker dest) {
		Logger.info(this, "Verifying bridge from %s - %s", source, dest);
		MqttClient publisher = null, subscriber = null;
		try {
			publisher = connect(source);
			subscriber = connect(dest);
			subscriber.setCallback(new MqttCallback() {

				@Override
				public void connectionLost(Throwable cause) {
					Logger.warn(this, "Connection lost");
				}

				@Override
				public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
					String message = new String(mqttMessage.getPayload(), "UTF-8");
					String expected = dest.getBrokerName() + "-" + source.getBrokerName();
					Logger.debug(this, "Message arrived. topic = %s, message = %s", topic, message);
					if (message.equals(expected)) {
						String bridge = dest.toString() + " - " + source.toString();
						Logger.info(BridgeVerifier.class, "Bridge %s OK.", bridge);
					}
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					Logger.trace(this, "Delievery completed");
				}

			});
			testMessageSend(publisher, dest, source);
		} catch (MqttException e) {
			e.printStackTrace();
		} finally {
			try {
				if (publisher != null) {
					publisher.disconnect();
				}
				if (subscriber != null) {
					subscriber.disconnect();
				}
			} catch (MqttException e) {
				e.printStackTrace();
			}
		}
	}

	private MqttClient connect(Broker broker) throws MqttSecurityException, MqttException {
		String serverUrl = "ssl://" + broker.getIp() + ":" + broker.getPort();
		Logger.debug(this, "Connecting to %s", serverUrl);
		MqttClient mqttClient = new MqttClient(serverUrl, "BridgeVerifier_" + broker.getBrokerName());
		MqttConnectOptions options = new MqttConnectOptions();
		options.setUserName(MQTT_BRIDGE_USERNAME);
		options.setPassword(MQTT_BRIDGE_PASSWORD.toCharArray());
		options.setCleanSession(true);
		options.setConnectionTimeout(CONNECTION_TIMEOUT);
		options.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
		AgentConfigurator agentConfigurator = agentService.getAgentConfigurator();
		SSLSocketFactory sslSocketFactory = new SSLSocketFactoryBuilder()
				.caCertificateFile(agentConfigurator.getCaCertificateFile())
				.clientCertificateFile(agentConfigurator.getClientCertificateFile())
				.keyFile(agentConfigurator.getKeyFile()).password(agentConfigurator.getBrokerPassword())
				.tlsVersion(agentConfigurator.getTlsVersion()).build();
		options.setSocketFactory(sslSocketFactory);
		mqttClient.setTimeToWait(MQTT_OPERATION_TIMEOUT);
		mqttClient.connect(options);
		List<Broker> connectedBrokers = this.broker.getConnectedTo();
		if (connectedBrokers != null && !connectedBrokers.isEmpty()) {
			for (Broker connectedBroker : connectedBrokers) {
				String topic = null;
				if (this.broker.getBrokerType().equals(Broker.BROKER_TYPE_LEVEL_L2)) {
					topic = "USER/" + connectedBroker.getBrokerName() + "/" + this.broker.getBrokerName()
							+ "/$BRIDGE_TEST";
				} else {
					topic = "USER/" + broker.getBrokerName() + "/$BRIDGE_TEST";
				}
				Logger.debug(this, "Subscribing to %s", topic);
				mqttClient.subscribe(topic);
			}
		}
		return mqttClient;
	}

	private void testMessageSend(MqttClient publisher, Broker source, Broker dest) {
		String topic = null;
		if (broker.getBrokerType().equals(Broker.BROKER_TYPE_LEVEL_L2)) {
			topic = "USER/" + dest.getBrokerName() + "/" + broker.getBrokerName() + "/$BRIDGE_TEST";
		} else {
			topic = "USER/" + broker.getBrokerName() + "/$BRIDGE_TEST";
		}
		String message = source.getBrokerName() + "-" + dest.getBrokerName();
		try {
			Logger.debug(this, "Publish topic: %s, message: %s on publisher %s", topic, message,
					publisher.getServerURI());
			publisher.publish(topic, new MqttMessage(message.getBytes()));
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}
