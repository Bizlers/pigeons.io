package com.bizlers.pigeons.api.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.paho.client.mqttv3.MqttClient;

import com.bizlers.pigeons.api.server.internal.ServerRegistrationAgent;
import com.bizlers.pigeons.api.server.persistent.models.PigeonV2;
import com.bizlers.utils.jaxrs.SecuredClientBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcabi.log.Logger;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class PublishPigeonPool {

	private static final long RECOVERY_INTERVAL = 60;

	private static final String URL_PIGEONS = "pigeons-server/rest/pigeons";
	
	private static PublishPigeonPool singleTon;
	
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

	private PigeonConnectionOperator connectionProvider;

	public static synchronized PublishPigeonPool load(PigeonServerUserConfig config) throws PigeonServerException {
		if (singleTon == null) {
			synchronized (PublishPigeonPool.class) {
				if (singleTon == null) {
					singleTon = createFromConfig(config);
				}
			}
		}
		return singleTon;
	}

	private static PublishPigeonPool createFromConfig(PigeonServerUserConfig config) throws PigeonServerException {
		Logger.info(PublishPigeonPool.class, "Requesting publish pigeons...");
		List<PigeonV2> pigeonList = null;
		long appId;
		try {
			Client client = new SecuredClientBuilder().keyStorePath(config.getKeyStorePath())
					.trustStorePath(config.getTrustStorePath()).keyStorePassword(config.getKeyStorePassword())
					.trustStorePassword(config.getTrustStorePassword())
					.privateKeyPassword(config.getPrivateKeyPassword()).build();
			Logger.debug(PublishPigeonPool.class, "Client to connect with pigeons server is created.");
			client.addFilter(new HTTPBasicAuthFilter(String.valueOf(config.getUserId()), config.getSessionId()));
			appId = ServerRegistrationAgent.INSTANCE.register(config).getAppId();
			WebResource webResource = client
					.resource("https://" + config.getPigeonServerHost() + ":" + config.getPigeonServerPort() + "/" + URL_PIGEONS);
			Logger.debug(PublishPigeonPool.class, "Using base URL '%s' to access pigeons server.", URL_PIGEONS);
			MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
			queryParams.add("publish", "true");
			queryParams.add("region", config.getRegion());
			queryParams.add("appId", String.valueOf(appId));
			ClientResponse response = webResource.queryParams(queryParams).get(ClientResponse.class);
			if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
				String errorMsg = response.getClientResponseStatus().getReasonPhrase();
				Logger.error(PublishPigeonPool.class, "HTTP Response  %d", response.getStatus());
				throw new PigeonServerException("Failed to retrieved the publish pigeon - " + errorMsg
						+ ".HTTP error code: " + response.getStatus());
			} else {
				InputStream inputStream = response.getEntityInputStream();
				try {
					pigeonList = new ObjectMapper().readValue(inputStream, new TypeReference<List<PigeonV2>>() {
					});
				} catch (IOException e) {
					Logger.error(PublishPigeonPool.class, "Exception  %[exception]s", e);
					throw new PigeonServerException("Failed to retrieve publish pigeons. - " + e.getMessage(), e);
				}
				Logger.info(PublishPigeonPool.class, "Sucessfully retrieved the publish pigeon.");
			}
		} catch (IOException e) {
			throw new PigeonServerException("Error in connecting with pigeons server", e);
		}
		PublishPigeonPool publishPigeonPool = null;
		if(pigeonList != null && !pigeonList.isEmpty()) {
			publishPigeonPool = new PublishPigeonPool();
			publishPigeonPool.setPigeonConnectionOperator(new PigeonConnectionOperator(appId, config));
			publishPigeonPool.initialize(pigeonList);
		} else {
			Logger.error(PublishPigeonPool.class, "pigeonList is empty.");
		}
		return publishPigeonPool;
	}
	
	private PublishPigeonPool() {
		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleWithFixedDelay(new ConnectionRecoveryTask(), RECOVERY_INTERVAL, RECOVERY_INTERVAL,
				TimeUnit.SECONDS);
	}

	public void setPigeonListener(PigeonListener pigeonListener) {
		connectionProvider.setPigeonListener(pigeonListener);
	}

	private void initialize(List<PigeonV2> pigeons) {
		pool = new ConcurrentLinkedQueue<MqttClient>();
		for (PigeonV2 pigeon : pigeons) {
			MqttClient mqttClient = connectionProvider.createConnection(pigeon);
			if (mqttClient != null) {
				pool.add(mqttClient);
			}
		}
	}

	public void setPigeonConnectionOperator(PigeonConnectionOperator pigeonConnectionOperator) {
		this.connectionProvider = pigeonConnectionOperator;
	}
	
	public PigeonConnectionOperator getPigeonConnectionOperator() {
		return connectionProvider;
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
		for (Iterator<MqttClient> iterator = pool.iterator(); iterator.hasNext();) {
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
			Logger.debug(this, "Running ConnectionRecoveryTask. Total Connections - " + pool.size());
			for (Iterator<MqttClient> iterator = pool.iterator(); iterator.hasNext();) {
				MqttClient mqttClient = (MqttClient) iterator.next();

				if (mqttClient.isConnected()) {
					Logger.trace(this, "Publisher connected - " + mqttClient.getClientId());
					continue;
				} else if (!mqttClient.isConnected()) {
					Logger.warn(this, "Publisher not connected. Will reconnect - " + mqttClient.getClientId());
					connectionProvider.reconnectMqttClient(mqttClient);
					if (!mqttClient.isConnected()) {
						MqttClient newMqttClient = connectionProvider.createConnection(mqttClient.getClientId());
						if (newMqttClient != null) {
							pool.remove(mqttClient);
							pool.add(newMqttClient);
						}
					}
				}
			}
		}
	}
}
