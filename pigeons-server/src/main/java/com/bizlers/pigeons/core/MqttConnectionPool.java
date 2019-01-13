package com.bizlers.pigeons.core;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClient;

import com.bizlers.pigeons.models.Pigeon;
import com.jcabi.log.Logger;

public class MqttConnectionPool {

	private static final long RECOVERY_INTERVAL = 60;

	/*
	 * Used ConcurrentLinkedQueue as it is thread-safe.
	 */
	private ConcurrentLinkedQueue<MqttClient> pool;
	private Object waitLock = new Object();

	/*
	 * 
	 * ScheduledExecutorService starts a special task in a separate thread and
	 * observes the validity of the connection in the pool periodically. When
	 * any of the connection expires it will replace the connection with new
	 * connection.
	 */
	private ScheduledExecutorService executorService;

	private MqttConnectionOperator connectionProvider;

	public MqttConnectionPool(List<Pigeon> pigeons,
			MqttConnectionOperator connectionProvider) {
		this.connectionProvider = connectionProvider;
		initialize(pigeons);

		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleWithFixedDelay(new ConnectionRecoveryTask(),
				RECOVERY_INTERVAL, RECOVERY_INTERVAL, TimeUnit.SECONDS);
	}

	private void initialize(List<Pigeon> pigeons) {
		pool = new ConcurrentLinkedQueue<MqttClient>();
		for (Pigeon pigeon : pigeons) {
			MqttClient mqttClient = connectionProvider.createConnection(pigeon
					.getClientId());
			if (mqttClient != null) {
				pool.add(mqttClient);
			}
		}
	}

	/*
	 * Gets the next free connection from the pool. If the pool doesn't contain
	 * any connection wait until connection is available.
	 * 
	 * @return PigeonConnection borrowed connection
	 */
	public synchronized MqttClient borrowObject() {
		while (pool.isEmpty()) {
			try {
				synchronized (waitLock) {
					waitLock.wait();
				}
			} catch (InterruptedException e) {
				Logger.warn(this, "Exception while waiting. %s", e.getMessage());
			}
		}
		return pool.poll();
	}

	/*
	 * Returns connection back to the pool.
	 * 
	 * @param PigeonConnection connection to be returned
	 */
	public void returnObject(MqttClient mqttClient) {
		if (mqttClient != null) {
			this.pool.offer(mqttClient);
			synchronized (waitLock) {
				waitLock.notify();
			}
		}
	}

	public int getSize() {
		return pool.size();
	}

	/*
	 * Check if any of the connection in the pool is connected. Return true if
	 * any one of the connection is connected. Return false otherwise.
	 */
	public boolean isConnected() {
		for (Iterator<MqttClient> iterator = pool.iterator(); iterator
				.hasNext();) {
			MqttClient mqttClient = (MqttClient) iterator.next();
			if (mqttClient != null && mqttClient.isConnected()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Shutdown this pool.
	 */
	public void shutdown() {
		while (!pool.isEmpty()) {
			connectionProvider.terminateConnection(pool.poll());
		}
		if (executorService != null) {
			executorService.shutdown();
		}
	}

	private class ConnectionRecoveryTask implements Runnable {

		@Override
		public void run() {
			Logger.debug(this,
					"Running ConnectionRecoveryTask. Total Connections - "
							+ pool.size());
			for (Iterator<MqttClient> iterator = pool.iterator(); iterator
					.hasNext();) {
				MqttClient mqttClient = (MqttClient) iterator.next();

				if (mqttClient.isConnected()) {
					Logger.trace(this,
							"Publisher connected - " + mqttClient.getClientId());
					continue;
				} else if (!mqttClient.isConnected()) {
					Logger.warn(this,
							"Publisher not connected. Will reconnect - "
									+ mqttClient.getClientId());
					connectionProvider.reconnectMqttClient(mqttClient);
					if (!mqttClient.isConnected()) {
						MqttClient newMqttClient = connectionProvider
								.createConnection(mqttClient.getClientId());
						if (newMqttClient != null) {
							pool.remove(mqttClient);
							pool.add(newMqttClient);
						}
					}
				}
			}

		}
	}

	public interface MqttConnectionOperator {

		public MqttClient createConnection(String clientId);

		public void reconnectMqttClient(MqttClient mqttClient);

		public void terminateConnection(MqttClient mqttClient);
	}
}
