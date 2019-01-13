package com.bizlers.pigeons.agent.core;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.bizlers.pigeons.agent.core.MessageListener.OnConnectionLostListener;
import com.bizlers.pigeons.commommodels.Broker;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

public enum AgentMonitor implements OnConnectionLostListener {
	INSTANCE;
	private MqttConnector monitor;

	@Loggable(value = Loggable.TRACE)
	public void startMonitor(Broker statusListener, AgentService agentService)
			throws MqttException, PigeonAgentException, IOException,
			GeneralSecurityException {
		monitor = new MqttConnector(statusListener.getIp(),
				statusListener.getPort(), statusListener.getAgentId() + "_SLC",
				this, agentService);
		if (monitor.isConnected()) {
			subscribeAll();
		}
	}

	public void subscribeAll() {
		monitor.subscribe(MqttConnector.TOPIC_PIGEON_STATUS);
		monitor.subscribe(MqttConnector.TOPIC_ADD_PIGEON);
		monitor.subscribe(MqttConnector.TOPIC_DESTROY_PIGEON);
		monitor.subscribe(MqttConnector.TOPIC_SIGNAL);
		monitor.subscribe(MqttConnector.TOPIC_PING);
	}

	public void unSubscribeAll() {
	}

	public void subscribe(String topic) {
		monitor.subscribe(topic);
	}

	public void publish(String topic, String message) {
		monitor.publish(topic, message);
	}

	public void stopMonitor() {
		unSubscribeAll();
		monitor.disconnect();
	}

	public void checkConnection() {
		if (!monitor.isConnected()) {
			Logger.info(this,
					"SLB Monitor is disconnected. Trying to reconnect.");
			monitor.reconnect();
			if (monitor.isConnected()) {
				subscribeAll();
			}
		} else {
			Logger.info(this, "SLB Monitor is connected.");
		}
	}

	@Override
	@Loggable(value = Loggable.TRACE, limit = 1, unit = TimeUnit.SECONDS)
	public void onConnectionLost() {
		monitor.reconnect();
		if (monitor.isConnected()) {
			subscribeAll();
		}
	}

}
