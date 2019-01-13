package com.bizlers.pigeons.agent.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import com.bizlers.auth.client.AuthClient;
import com.bizlers.auth.client.AuthClientException;
import com.bizlers.auth.client.AuthConfig;
import com.bizlers.auth.client.Session;
import com.bizlers.auth.client.jaxrs.JAXRSAuthClient;
import com.bizlers.pigeons.agent.core.BrokerAccessController.BrokerAccessControlCallback;
import com.bizlers.pigeons.agent.utils.SystemInformation;
import com.bizlers.pigeons.commommodels.Agent;
import com.bizlers.pigeons.commommodels.Broker;
import com.bizlers.pigeons.commommodels.Pigeon;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

import expectj.ExpectJ;
import expectj.ExpectJException;
import expectj.Spawn;
import expectj.TimeoutException;

public class AgentService implements BrokerAccessControlCallback {

	public static final int KEEP_ALIVE_INTERVAL = 60 * 15 * 1000; // 10 Minutes

	public static final int MAX_NUMBER_OF_CLIENT = 1000;

	public static final int MAX_RETRY_COUNT = 5;

	public static final String RELOAD_ACTION_ADD = "A";

	public static final String RELOAD_ACTION_DELETE = "D";

	private static final String DEFAULT_AUTH_CONFIG_FILE = "auth_client.properties";

	private Broker brokerStatusListener;

	private Agent agent;

	private AgentConfigurator agentConfigurator;

	private Session session;

	private PigeonServerUser pigeonServerUser;

	private RecoveryScheduler recoveryScheduler;

	/*
	 * List of brokers registered on the pigeons server and are running on
	 * current agent.
	 */
	private Map<Integer, String> brokerList;

	/*
	 * Pigeons that are ready for connection.
	 */
	private Map<Integer, List<String>> availablePigeonList;

	/*
	 * Pigeons that are currently connected and are online.
	 */
	private Map<Integer, DelayQueue<Connection>> onlinePigeonList;

	/*
	 * Pigeons that has been added to password file and are supposed to be
	 * updated on pigeon server, to be ready to use, after successful
	 * configuration reload.
	 */
	private List<String> pigeonsReadyToUse;

	/*
	 * Count of the pigeons ready to use, grouped by port.
	 */
	private Map<Integer, Integer> readyToUsePigeonsCount;

	/*
	 * Pigeons that has been removed from password file and are supposed to be
	 * delete from pigeon server after successful configuration reload.
	 */
	private List<String> pigeonsDestroyed;

	/*
	 * Count of pigeons that has been destroyed, grouped by port.
	 */
	private Map<Integer, Integer> destroyedPigeonsCount;

	/*
	 * Pigeons that has been successfully connected (are online) but could not
	 * be updated on pigeon server to change the status to be online.
	 */
	private List<String> pigeonsPendingForConnectUpdate;

	/*
	 * Pigeons that has been successfully disconnected (are offline/allocated)
	 * but could not be updated on pigeon server to change the status to be
	 * allocated.
	 */
	private List<String> pigeonsPendingForReallocationUpdate;

	/*
	 * Pigeons pending for connect update that can not be added to
	 * pigeonsPendingForConnectUpdate list because update scheduler was already
	 * running.
	 */
	private List<String> tempPigeonsPendingForConnectUpdate;

	/*
	 * Pigeons pending for reallocation update that can not be added to
	 * pigeonsPendingForReallocationUpdate list because update scheduler was
	 * already running.
	 */
	private List<String> tempPigeonsPendingForReallocationUpdate;

	/*
	 * List of broker access Controllers for each broker.
	 */
	private Map<Integer, BrokerAccessController> brokerAccessControllers;

	private List<Broker> bridgeBrokers = new ArrayList<>();

	/*
	 * List of update call once at a time per pigeon.
	 */
	private static Map<String, Object> pigeonUpdateLocks;

	@Loggable(value = Loggable.DEBUG)
	public AgentService() {

		pigeonUpdateLocks = new ConcurrentHashMap<String, Object>();

		brokerList = new ConcurrentHashMap<Integer, String>();

		availablePigeonList = new ConcurrentHashMap<Integer, List<String>>();

		onlinePigeonList = new ConcurrentHashMap<Integer, DelayQueue<Connection>>();

		readyToUsePigeonsCount = new ConcurrentHashMap<Integer, Integer>();

		destroyedPigeonsCount = new ConcurrentHashMap<Integer, Integer>();

		pigeonsReadyToUse = Collections
				.synchronizedList(new ArrayList<String>());

		pigeonsDestroyed = Collections
				.synchronizedList(new ArrayList<String>());

		pigeonsPendingForConnectUpdate = Collections
				.synchronizedList(new ArrayList<String>());

		pigeonsPendingForReallocationUpdate = Collections
				.synchronizedList(new ArrayList<String>());

		tempPigeonsPendingForConnectUpdate = Collections
				.synchronizedList(new ArrayList<String>());

		tempPigeonsPendingForReallocationUpdate = Collections
				.synchronizedList(new ArrayList<String>());

		agentConfigurator = new AgentConfigurator(this);

		brokerAccessControllers = new ConcurrentHashMap<Integer, BrokerAccessController>();
	}

	public PigeonServerUser getPigeonServerUser() {
		return pigeonServerUser;
	}

	public Broker getBrokerStatusListener() {
		return brokerStatusListener;
	}

	public void setBrokerStatusListener(Broker brokerStatusListener) {
		this.brokerStatusListener = brokerStatusListener;
	}

	public Map<Integer, String> getBrokerList() {
		return brokerList;
	}

	public void setBrokerList(Map<Integer, String> brokerList) {
		this.brokerList = brokerList;
	}

	public Map<Integer, Integer> getReadyToUsePigeonsCount() {
		return readyToUsePigeonsCount;
	}

	public void setReadyToUsePigeonsCount(
			Map<Integer, Integer> readyToUsePigeonsCount) {
		this.readyToUsePigeonsCount = readyToUsePigeonsCount;
	}

	public Map<Integer, Integer> getDestroyedPigeonsCount() {
		return destroyedPigeonsCount;
	}

	public void setDestroyedPigeonsCount(
			Map<Integer, Integer> destroyedPigeonsCount) {
		this.destroyedPigeonsCount = destroyedPigeonsCount;
	}

	public List<String> getPigeonsPendingForConnectUpdate() {
		return pigeonsPendingForConnectUpdate;
	}

	public void setPigeonsPendingForConnectUpdate(
			List<String> pigeonsPendingForConnectUpdate) {
		this.pigeonsPendingForConnectUpdate = pigeonsPendingForConnectUpdate;
	}

	public List<String> getPigeonsPendingForReallocationUpdate() {
		return pigeonsPendingForReallocationUpdate;
	}

	public void setPigeonsPendingForReallocationUpdate(
			List<String> pigeonsPendingForReallocationUpdate) {
		this.pigeonsPendingForReallocationUpdate = pigeonsPendingForReallocationUpdate;
	}

	public List<String> getTempPigeonsPendingForConnectUpdate() {
		return tempPigeonsPendingForConnectUpdate;
	}

	public void setTempPigeonsPendingForConnectUpdate(
			List<String> tempPigeonsPendingForConnectUpdate) {
		this.tempPigeonsPendingForConnectUpdate = tempPigeonsPendingForConnectUpdate;
	}

	public List<String> getTempPigeonsPendingForReallocationUpdate() {
		return tempPigeonsPendingForReallocationUpdate;
	}

	public void setTempPigeonsPendingForReallocationUpdate(
			List<String> tempPigeonsPendingForReallocationUpdate) {
		this.tempPigeonsPendingForReallocationUpdate = tempPigeonsPendingForReallocationUpdate;
	}

	public AgentConfigurator getAgentConfigurator() {
		return agentConfigurator;
	}

	public void setAgentConfigurator(AgentConfigurator agentConfigurator) {
		this.agentConfigurator = agentConfigurator;
	}

	public Map<Integer, BrokerAccessController> getBrokerAccessControllers() {
		return brokerAccessControllers;
	}

	public BrokerAccessController getBrokerAccessController(int port) {
		return brokerAccessControllers.get(port);
	}

	public void initialise(String path) throws PigeonAgentException,
			AuthClientException {
		try {
			agentConfigurator.setConfigFilePath(path);
			agentConfigurator.loadAgentConfiguration();
			if (agentConfigurator != null
					&& isAgentRunning(agentConfigurator.getConfigFilePath())) {
				throw new PigeonAgentException(
						"Agent is already  running on your machine with given configuration.");
			}
			ConfigurationValidator.validate(agentConfigurator);
			session = getSession();
			pigeonServerUser = new PigeonServerUser(agentConfigurator, session,
					this);
			agent = getAgent();
		} catch (InterruptedException | IOException | ExpectJException
				| TimeoutException | GeneralSecurityException e) {
			throw new PigeonAgentException(
					"Exception while initializing Agent.", e);
		}
	}

	/*
	 * Register and get the current agent on pigeons server.
	 */
	private Agent getAgent() throws PigeonAgentException {
		Agent agent = new Agent(0, session.getAccountId(),
				agentConfigurator.getAgentName(),
				agentConfigurator.getHostName(),
				agentConfigurator.getHostIpAddress(), Agent.STATUS_ONLINE,
				agentConfigurator.getStatusListenerBrokerPort());

		Agent existingAgent = pigeonServerUser.getAgent(agent.getIp(),
				agent.getAgentName());
		if (existingAgent == null) {
			Logger.info(this, "creating new Agent.");
			pigeonServerUser.registerAgent(agent);
		} else {
			Logger.info(this,
					"Agent already exists with IP %s Status Listner Port %d.",
					existingAgent.getIp(),
					existingAgent.getStatusListenerBrokerPort());
			if (existingAgent.getStatus().equalsIgnoreCase(Agent.STATUS_ONLINE)) {
				agent = existingAgent;
				deactivateAllBrokers();
				deactivateAgent(agent.getIp(), agent.getAgentName());
				existingAgent = pigeonServerUser.getAgent(agent.getIp(),
						agent.getAgentName());
			}
			Logger.info(this, "making Agent ONLINE.");
			existingAgent.setStatus(Agent.STATUS_ONLINE);
			pigeonServerUser.updateAgent(existingAgent);
			Logger.info(this, "Agent is ONLINE now.");
			agent = existingAgent;
		}
		return agent;
	}

	/*
	 * Initialize a session for current agent on pigeons server.
	 */
	private Session getSession() throws PigeonAgentException,
			AuthClientException, IOException {
		AuthConfig authConfig = AuthConfig.load(DEFAULT_AUTH_CONFIG_FILE);

		AuthClient authClient = JAXRSAuthClient.newInstance(authConfig);
		Session session = null;
		try {
			if (!authClient.accountExists(agentConfigurator.getAgentUserName())) {
				authClient.signUp(agentConfigurator.getAgentUserName(),
						agentConfigurator.getAgentUserName(),
						agentConfigurator.getAgentPassword());
				authClient.activateAccount(
						agentConfigurator.getAgentUserName(),
						agentConfigurator.getAgentUserName(),
						agentConfigurator.getAgentPassword());
			} else if (!authClient.isAccountActivated(agentConfigurator
					.getAgentUserName())) {
				authClient.activateAccount(
						agentConfigurator.getAgentUserName(),
						agentConfigurator.getAgentUserName(),
						agentConfigurator.getAgentPassword());
			}

			if (!authClient.userHasAccess(agentConfigurator.getAgentUserName(),
					agentConfigurator.getAgentPassword(),
					agentConfigurator.getAppName())) {
				session = authClient.signIn(
						agentConfigurator.getAgentUserName(),
						agentConfigurator.getAgentPassword(),
						agentConfigurator.getDeviceId());
				authClient.grantAccess(agentConfigurator.getAppName(), session);
			}
			session = authClient.signIn(agentConfigurator.getAgentUserName(),
					agentConfigurator.getAgentPassword(),
					agentConfigurator.getDeviceId());
		} catch (AuthClientException e) {
			Logger.error(this,
					"Failed to initialize session. Exception: %[exception]s", e);
			throw new PigeonAgentException("Failed to initialize session.");
		}
		return session;
	}

	@Loggable(value = Loggable.DEBUG)
	public void startService() throws PigeonAgentException {
		startStatusListenerBroker();
		if (agentConfigurator.isStartL0() == true) {
			startBrokerL0();
		}
		if (agentConfigurator.isStartL1() == true) {
			startBrokersL1();
		}
		if (agentConfigurator.isStartL2() == true) {
			startBrokersL2();
		}
		checkAllBrokers();
		recoveryScheduler = new RecoveryScheduler(this);
	}

	@Loggable(value = Loggable.DEBUG, limit = 5, unit = TimeUnit.SECONDS)
	public void stopService() throws PigeonAgentException {
		try {
			recoveryScheduler.terminateScheduler();
			Set<Integer> portList = brokerAccessControllers.keySet();
			for (Iterator<Integer> iterator = portList.iterator(); iterator
					.hasNext();) {
				int port = (Integer) iterator.next();
				stopBroker(port);
				deleteConfigurationAndLogsForBroker(port);
				deactivateBroker(port);
			}
			deactivateAgent(agent.getIp(), agent.getAgentName());
			deleteConfigurationAndLogsForBroker(agentConfigurator
					.getStatusListenerBrokerPort());

			JAXRSAuthClient.newInstance(
					AuthConfig.load(DEFAULT_AUTH_CONFIG_FILE)).signOut(session);
		} catch (AuthClientException e) {
			throw new PigeonAgentException(
					"Exception while stopping agent service", e);
		} catch (IOException e) {
			throw new PigeonAgentException(
					"Exception while stopping agent service", e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	private void startStatusListenerBroker() throws PigeonAgentException {
		deleteConfigurationAndLogsForBroker(agentConfigurator
				.getStatusListenerBrokerPort());
		brokerStatusListener = new Broker(agent.getAgentId(),
				agentConfigurator.getHostIpAddress(),
				agentConfigurator.getStatusListenerBrokerPort(), 1, 0, 1,
				agentConfigurator.getRegion(), Broker.STATUS_ONLINE,
				agent.getAgentName() + "_" + "STATUS_LISTENER",
				Broker.BROKER_TYPE_STATUS_LISTENER, 0);
		startBroker(brokerStatusListener, null);
	}

	@Loggable(value = Loggable.DEBUG)
	private void deactivateAllBrokers() throws PigeonAgentException {
		List<Integer> portList = new ArrayList<>();

		portList.addAll(agentConfigurator.getBrokerPortArrayListL0());
		portList.addAll(agentConfigurator.getBrokerPortArrayListL1());
		portList.addAll(agentConfigurator.getBrokerPortArrayListL2());

		for (Integer port : portList) {
			Broker oldBroker = pigeonServerUser.getBroker(
					agentConfigurator.getHostIpAddress(), port);
			if (oldBroker != null
					&& oldBroker.getStatus().equalsIgnoreCase(
							Broker.STATUS_ONLINE)) {
				oldBroker.setStatus(Broker.STATUS_OFFLINE);
				pigeonServerUser.updateBroker(oldBroker);
			}
		}
	}

	@Loggable(value = Loggable.DEBUG)
	private void startBrokerL0() throws PigeonAgentException {
		List<Broker> brokers = null;
		brokers = pigeonServerUser.getBrokers(Broker.BROKER_TYPE_LEVEL_L1);

		List<Integer> portList = agentConfigurator.getBrokerPortArrayListL0();
		for (Integer port : portList) {
			deleteConfigurationAndLogsForBroker(port);
			Broker broker = new Broker(agent.getAgentId(),
					agentConfigurator.getHostIpAddress(), port,
					MAX_NUMBER_OF_CLIENT, 0, 1000,
					agentConfigurator.getRegion(), Broker.STATUS_ONLINE, null,
					brokers, Broker.BROKER_TYPE_LEVEL_L0, 0);
			Broker oldBroker = pigeonServerUser.getBroker(broker.getIp(),
					broker.getPort());

			if (oldBroker == null) {
				Logger.info(this, "Creating new Broker.");
				pigeonServerUser.registerBroker(broker);
			} else {
				Logger.info(this,
						" Broker %s already exists with IP %s and Port %d.",
						oldBroker.getBrokerName(), oldBroker.getIp(),
						oldBroker.getPort());
				if (oldBroker.getStatus().equalsIgnoreCase(
						Broker.STATUS_OFFLINE)) {
					Logger.info(this, "making Broker ONLINE.");
					oldBroker.setStatus(Broker.STATUS_ONLINE);
					oldBroker.setConnectedTo(brokers);
					oldBroker.setAgentId(broker.getAgentId());
					oldBroker.setBrokerType(broker.getBrokerType());
					oldBroker.setMaxLimit(broker.getMaxLimit());
					pigeonServerUser.updateBroker(oldBroker);
					Logger.info(this, "Broker is ONLINE now.");
				} else {
					Logger.error(this, "Broker is already ONLINE.");
				}
				broker = oldBroker;
			}
			bridgeBrokers.add(broker);
			startBrokerWithBridges(broker, brokers);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	private void startBrokersL1() throws PigeonAgentException {
		List<Broker> brokers = null;
		brokers = pigeonServerUser.getBrokers(Broker.BROKER_TYPE_LEVEL_L0);
		if (brokers != null) {
			List<Integer> portList = agentConfigurator
					.getBrokerPortArrayListL1();
			for (Integer port : portList) {
				deleteConfigurationAndLogsForBroker(port);
				Broker broker = new Broker(agent.getAgentId(),
						agentConfigurator.getHostIpAddress(), port,
						MAX_NUMBER_OF_CLIENT, 0, 1000,
						agentConfigurator.getRegion(), Broker.STATUS_ONLINE,
						null, brokers, Broker.BROKER_TYPE_LEVEL_L1, 0);
				Broker oldBroker = pigeonServerUser.getBroker(broker.getIp(),
						broker.getPort());
				if (oldBroker == null) {
					Logger.info(this, " creating new Broker .");
					pigeonServerUser.registerBroker(broker);
				} else {
					Logger.info(
							this,
							" Broker %s already exists with IP %s and Port %d.",
							oldBroker.getBrokerName(), oldBroker.getIp(),
							oldBroker.getPort());
					if (oldBroker.getStatus().equalsIgnoreCase(
							Broker.STATUS_OFFLINE)) {
						Logger.info(this, "making Broker ONLINE.");
						oldBroker.setStatus(Broker.STATUS_ONLINE);
						oldBroker.setConnectedTo(brokers);
						oldBroker.setAgentId(broker.getAgentId());
						oldBroker.setBrokerType(broker.getBrokerType());
						oldBroker.setMaxLimit(broker.getMaxLimit());
						pigeonServerUser.updateBroker(oldBroker);
						Logger.info(this, "Broker is ONLINE now.");
					} else {
						Logger.error(this, "Broker is already ONLINE.");
					}
					broker = oldBroker;
				}
				bridgeBrokers.add(broker);
				startBrokerWithBridges(broker, brokers);
			}
		} else {
			throw new PigeonAgentException(
					"Cannot start broker at L1. No L0 broker found.");
		}
	}

	@Loggable(value = Loggable.DEBUG)
	private void startBrokersL2() throws PigeonAgentException {
		List<Integer> portList = agentConfigurator.getBrokerPortArrayListL2();
		for (Integer port : portList) {
			deleteConfigurationAndLogsForBroker(port);
			Broker parentBroker = pigeonServerUser.getBroker(
					Broker.BROKER_TYPE_LEVEL_L1, null);
			if (parentBroker != null) {
				List<Broker> brokers = new ArrayList<>();
				brokers.add(parentBroker);
				Broker broker = null;
				if (agentConfigurator.isRedirectPortAvailable()
						&& agentConfigurator.getRedirectDestinationPort() == port) {
					broker = new Broker(agent.getAgentId(),
							agentConfigurator.getHostIpAddress(), port,
							MAX_NUMBER_OF_CLIENT, 0, 1000,
							agentConfigurator.getRegion(),
							Broker.STATUS_ONLINE, null, brokers,
							Broker.BROKER_TYPE_LEVEL_L2,
							agentConfigurator.getRedirectPort());

				} else {
					broker = new Broker(agent.getAgentId(),
							agentConfigurator.getHostIpAddress(), port,
							MAX_NUMBER_OF_CLIENT, 0, 1000,
							agentConfigurator.getRegion(),
							Broker.STATUS_ONLINE, null, brokers,
							Broker.BROKER_TYPE_LEVEL_L2, 0);
				}
				Broker oldBroker = pigeonServerUser.getBroker(broker.getIp(),
						broker.getPort());
				if (oldBroker == null) {
					Logger.info(this, " creating new Broker .");
					pigeonServerUser.registerBroker(broker);
				} else {
					Logger.info(
							this,
							" Broker %s already exists with IP %s and Port %d.",
							oldBroker.getBrokerName(), oldBroker.getIp(),
							oldBroker.getPort());
					if (oldBroker.getStatus().equalsIgnoreCase(
							Broker.STATUS_OFFLINE)) {
						Logger.info(this, "making Broker ONLINE.");
						oldBroker.setStatus(Broker.STATUS_ONLINE);
						oldBroker.setConnectedTo(brokers);
						oldBroker.setAgentId(broker.getAgentId());
						oldBroker.setBrokerType(broker.getBrokerType());
						oldBroker.setMaxLimit(broker.getMaxLimit());
						oldBroker.setRedirectPort(broker.getRedirectPort());
						pigeonServerUser.updateBroker(oldBroker);
						Logger.info(this, "Broker is ONLINE now.");
					} else {
						Logger.error(this, "Broker is already ONLINE.");
					}
					broker = oldBroker;
				}
				bridgeBrokers.add(broker);
				startBroker(broker, parentBroker);
			} else {
				throw new PigeonAgentException(
						"Cannot start broker at level 2. L1 Does not exists");
			}
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void startBrokerWithBridges(Broker currentBroker,
			List<Broker> brokerList) throws PigeonAgentException {
		try {
			int pid = 0;
			if (System.getProperty("os.name").equals("Linux")) {
				String configFileName = agentConfigurator
						.getAgentInstallationDir()
						+ "/config/config_"
						+ currentBroker.getPort() + ".conf";

				String logFileName = agentConfigurator
						.getAgentInstallationDir()
						+ "/logs/log_"
						+ currentBroker.getPort() + ".log";
				String[] finalCommand;
				if (new File("./target/classes/defaultConfig.conf").exists()) {
					String[] command = {
							"expect",
							"./Mosquitto/start_mosquitto.exp",
							"./Mosquitto/start.sh",
							agentConfigurator.getMqttServerInstallationDir()
									+ "mosquitto", "", logFileName,
							agentConfigurator.getBrokerPassword() };
					finalCommand = command;
				} else {
					String[] command = {
							"expect",
							agentConfigurator.getAgentInstallationDir()
									+ "/Mosquitto/start_mosquitto.exp",
							agentConfigurator.getAgentInstallationDir()
									+ "/Mosquitto/start.sh",
							agentConfigurator.getMqttServerInstallationDir()
									+ "mosquitto", "", logFileName,
							agentConfigurator.getBrokerPassword() };
					finalCommand = command;
				}
				File configFile = new File(configFileName);
				if (!configFile.exists()) {

					agentConfigurator.createDefaultConfigurationFile(
							currentBroker, null, brokerList);

					// Add default user credentials to the password file
					addToPasswordFile(currentBroker.getPort(),
							MqttConnector.MQTT_BRIDGE_USERNAME,
							MqttConnector.MQTT_BRIDGE_PASSWORD);
				}
				finalCommand[4] = configFileName;

				Runtime.getRuntime().exec(finalCommand);
				pid = SystemInformation.getProcessId2(agentConfigurator,
						currentBroker.getPort());
			}
			createBrokerAccessController(currentBroker.getPort(), pid);
		} catch (IOException | ExpectJException | TimeoutException
				| InterruptedException e) {
			throw new PigeonAgentException(
					"Failed to start broker with bridges at port: "
							+ currentBroker.getPort() + ". " + e.getMessage(),
					e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void startBroker(Broker currentBroker, Broker parentBroker)
			throws PigeonAgentException {
		try {
			int pid = 0;
			if (System.getProperty("os.name").equals("Linux")) {
				String configFileName = agentConfigurator
						.getAgentInstallationDir()
						+ "/config/config_"
						+ currentBroker.getPort() + ".conf";

				String logFileName = agentConfigurator
						.getAgentInstallationDir()
						+ "/logs/log_"
						+ currentBroker.getPort() + ".log";
				String[] finalCommand;
				if (new File("./target/classes/defaultConfig.conf").exists()) {
					String[] command = {
							"expect",
							"./Mosquitto/start_mosquitto.exp",
							"./Mosquitto/start.sh",
							agentConfigurator.getMqttServerInstallationDir()
									+ "mosquitto", "", logFileName,
							agentConfigurator.getBrokerPassword() };
					finalCommand = command;
				} else {
					String[] command = {
							"expect",
							agentConfigurator.getAgentInstallationDir()
									+ "/Mosquitto/start_mosquitto.exp",
							agentConfigurator.getAgentInstallationDir()
									+ "/Mosquitto/start.sh",
							agentConfigurator.getMqttServerInstallationDir()
									+ "mosquitto", "", logFileName,
							agentConfigurator.getBrokerPassword() };
					finalCommand = command;
				}
				File configFile = new File(configFileName);
				if (!configFile.exists()) {
					agentConfigurator.createDefaultConfigurationFile(
							currentBroker, parentBroker, null);

					// Add default user credentials to the password file
					addToPasswordFile(currentBroker.getPort(),
							MqttConnector.MQTT_BRIDGE_USERNAME,
							MqttConnector.MQTT_BRIDGE_PASSWORD);
				}

				finalCommand[4] = configFileName;
				Runtime.getRuntime().exec(finalCommand);
				pid = SystemInformation.getProcessId2(agentConfigurator,
						currentBroker.getPort());
			}
			createBrokerAccessController(currentBroker.getPort(), pid);
		} catch (IOException | ExpectJException | TimeoutException
				| InterruptedException e) {
			throw new PigeonAgentException("Failed to start broker at port: "
					+ currentBroker.getPort() + ". " + e.getMessage(), e);
		}
	}

	public void createBrokerAccessController(int port, int pid) {
		Logger.info(this,
				"Creating broker access Controller. Port : %d  processId %d ",
				port, pid);
		String mosquittoPasswordUtil = agentConfigurator
				.getMqttServerInstallationDir() + "mosquitto_passwd";
		String passwordFile = agentConfigurator.getAgentInstallationDir()
				+ "/config/pwfile_" + port + ".txt ";
		brokerAccessControllers.put(port, new BrokerAccessController(
				mosquittoPasswordUtil, passwordFile, port, pid));
	}

	@Loggable(value = Loggable.DEBUG)
	public void stopBroker(int port) {
		int processId = brokerAccessControllers.get(port).getProcessId();
		try {
			System.out.println("Killing Port : " + port + " Process ID "
					+ processId);
			if (processId != 0) {
				Runtime.getRuntime().exec("kill -9 " + processId);
			}
		} catch (IOException e) {
			Logger.warn(this, "Failed to stop broker at port: " + port
					+ ". Exception %[exception]s", e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void checkAllBrokers() throws PigeonAgentException {
		List<Integer> openPortList = new ArrayList<>();
		List<Integer> portList = new ArrayList<Integer>();
		if (agentConfigurator.isStartL0() == true) {
			portList.addAll(agentConfigurator.getBrokerPortArrayListL0());
		}
		if (agentConfigurator.isStartL1() == true) {
			portList.addAll(agentConfigurator.getBrokerPortArrayListL1());
		}
		if (agentConfigurator.isStartL2() == true) {
			portList.addAll(agentConfigurator.getBrokerPortArrayListL2());
		}
		portList.add(agentConfigurator.getStatusListenerBrokerPort());

		for (Integer port : portList) {
			if (!SystemInformation.isPortBusy(port)) {
				openPortList.add(port);
			}
		}

		if (!openPortList.isEmpty()) {
			Logger.error(this, " Brokers not running on Ports  : %s",
					openPortList);
			throw new PigeonAgentException(
					" Brokers not running on Ports. Please check broker default configuration file parameters.");
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deactivateAgent(String ipAddress, String agentName) {
		try {
			agent = pigeonServerUser.getAgent(ipAddress, agentName);
			if (agent != null) {
				agent.setStatus(Agent.STATUS_OFFLINE);
				pigeonServerUser.updateAgent(agent);
			}
		} catch (PigeonAgentException e) {
			Logger.warn(this, "Not able to deactivate the agent %s",
					e.getMessage());
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deactivateBroker(int port) {
		try {
			Broker broker = pigeonServerUser.getBrokerByName(getBrokerList()
					.get(port));
			if (broker != null) {
				broker.setStatus(Broker.STATUS_OFFLINE);
				pigeonServerUser.updateBroker(broker);
			}
		} catch (PigeonAgentException e) {
			Logger.warn(this,
					"Not able to deactivate Broker with port %d :  %s ", port,
					e.getMessage());
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deleteConfigurationAndLogsForBroker(int port) {
		String configFileName = agentConfigurator.getAgentInstallationDir()
				+ "/config/config_" + port + ".conf";
		new File(configFileName).delete();
		String logFileName = agentConfigurator.getAgentInstallationDir()
				+ "/logs/log_" + port + ".log";
		new File(logFileName).delete();
		String passwordFilePath = agentConfigurator.getAgentInstallationDir()
				+ "/config/pwfile_" + port + ".txt";
		new File(passwordFilePath).delete();
		String dbFilePath = agentConfigurator.getAgentInstallationDir()
				+ "/config/brokerstore_" + port + ".db";
		new File(dbFilePath).delete();
	}

	@Loggable(value = Loggable.DEBUG)
	private boolean isAgentRunning(String configFilePath) {
		try {
			ExpectJ expectinator = new ExpectJ(100);
			Spawn shell = expectinator.spawn("/bin/bash");
			String command = "ps -ef | grep " + configFilePath
					+ " | grep -c java ";
			shell.send(command + "\n");
			sleep(5);
			String pid = shell.getCurrentStandardOutContents();
			shell.stop();
			shell.expectClose();
			if (Integer.parseInt(pid.trim()) >= 2) {
				return true;
			}
		} catch (IOException | ExpectJException | TimeoutException e) {
			Logger.error(this,
					"Failed to check if agent is running. " + e.getMessage());
		}
		return false;
	}

	@Loggable(value = Loggable.TRACE)
	private List<Integer> getMosquittoProcessList() throws IOException {
		List<Integer> list = new ArrayList<Integer>();
		Process process = Runtime.getRuntime().exec(
				"cmd /c tasklist | find \"mosquitto.exe\" | sort");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				process.getInputStream()));

		String line = null;
		while ((line = reader.readLine()) != null) {
			StringTokenizer tokanizer = new StringTokenizer(line);
			tokanizer.nextToken();
			String processId = tokanizer.nextToken();
			list.add(Integer.parseInt(processId));
		}
		reader.close();
		return list;
	}

	@Loggable(value = Loggable.DEBUG)
	public void processPing(int port, String clientId, int clientApiVersion) {
		Connection connection = new Connection(port, clientId,
				System.currentTimeMillis(), clientApiVersion);
		DelayQueue<Connection> connections = onlinePigeonList.get(port);
		if (connections != null && !connections.isEmpty()) {
			if (connections.contains(connection)) {
				connections.remove(connection);
			}
			connections.put(connection);
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 5, unit = TimeUnit.SECONDS)
	public void onClientConnected(String clientId, int port,
			int clientApiVersion) throws PigeonAgentException {
		try {
			clientConnected(clientId, port, clientApiVersion, false);
		} catch (UniformInterfaceException | ClientHandlerException
				| NumberFormatException | PigeonAgentException e1) {
			if (!recoveryScheduler.isRunning()) {
				getPigeonsPendingForConnectUpdate().add(
						clientId + ":" + port + ":" + clientApiVersion);
			} else {
				getTempPigeonsPendingForConnectUpdate().add(
						clientId + ":" + port + ":" + clientApiVersion);
			}
			Logger.warn(this,
					"Exception occured while adding pigeon:  %[exception]s", e1);
			return;
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 5, unit = TimeUnit.SECONDS)
	public void clientConnected(String clientId, int port,
			int clientApiVersion, boolean isPendingCall)
			throws PigeonAgentException {
		updatePigeon(clientId, clientApiVersion, port, isPendingCall, false);
	}

	@Loggable(value = Loggable.TRACE)
	private boolean isValidClient(String clientId, int port) {
		if (((availablePigeonList.containsKey(port)) && availablePigeonList
				.get(port).contains(clientId))
				|| ((onlinePigeonList.containsKey(port)) && onlinePigeonList
						.get(port).contains(new Connection(clientId)))) {
			return true;
		}
		return false;
	}

	/*
	 * Called whenever any client gets disconnected.
	 */
	@Loggable(value = Loggable.DEBUG)
	public void onClientDisconnect(String clientId, int port) {
		Logger.info(this,
				"Reallocating disconnected pigeon. Port: %d, Client ID: %s",
				port, clientId);
		try {
			reallocatePigeon(clientId, port, false);
		} catch (PigeonAgentException e) {
			if (!recoveryScheduler.isRunning()) {
				getPigeonsPendingForReallocationUpdate().add(
						clientId + ":" + port);
			} else {
				getTempPigeonsPendingForReallocationUpdate().add(
						clientId + ":" + port);
			}
			Logger.warn(this,
					"Exception occured while removing pigeon:  %[exception]s",
					e);
		}
	}

	/*
	 * Change pigeon status back to allocated from online and make pigeon
	 * available to reconnect.
	 */
	@Loggable(value = Loggable.DEBUG)
	public void reallocatePigeon(String clientId, int port, boolean isPending)
			throws PigeonAgentException {
		updatePigeon(clientId, 0, port, isPending, true);
	}

	public synchronized void insertInPigeonLocks(String clientId) {
		if (!pigeonUpdateLocks.containsKey(clientId)) {
			pigeonUpdateLocks.put(clientId, new Object());
		}
	}

	public synchronized void removeFromPigeonLocks(String clientId) {
		pigeonUpdateLocks.remove(clientId);
	}

	@Loggable(value = Loggable.DEBUG)
	public void updatePigeon(String clientId, int clientApiVersion, int port,
			boolean isPending, boolean reallocate) throws PigeonAgentException {
		if (reallocate) {
			insertInPigeonLocks(clientId);
			synchronized (pigeonUpdateLocks.get(clientId)) {
				Pigeon pigeon = pigeonServerUser.getPigeon(clientId);
				if (pigeon != null) {
					pigeon.setStatus(Pigeon.STATUS_ALLOTED);
					pigeonServerUser.updatePigeon(pigeon, isPending);
					removeFromQueue(onlinePigeonList, clientId, port);
					addToList(availablePigeonList, clientId, port);
				} else {
					Logger.warn(this,
							"Failed to reallocate pigeon. No such pigeon exists. Client ID:"
									+ clientId);
				}
			}
			removeFromPigeonLocks(clientId);
		} else {
			insertInPigeonLocks(clientId);
			synchronized (pigeonUpdateLocks.get(clientId)) {
				if (isValidClient(clientId, port)) {
					Pigeon pigeon = pigeonServerUser.getPigeon(clientId);
					if (pigeon == null) {
						Logger.warn(this,
								"Pigeon doesn't exists on server with ClientId :"
										+ clientId);
						return;
					}
					pigeon.setStatus(Pigeon.STATUS_ONLINE);
					pigeonServerUser.updatePigeon(pigeon, isPending);
					removeFromList(availablePigeonList, clientId, port);
					addToQueue(onlinePigeonList, clientId, port,
							clientApiVersion);
				} else {
					// FIXME Overwrite the connected if it is invalid
				}
			}
			removeFromPigeonLocks(clientId);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void addNewPigeon(String clientId, int port, String userName,
			String password) throws IOException, ExpectJException,
			TimeoutException {
		brokerAccessControllers.get(port).add(this, clientId, userName,
				password);
	}

	@Loggable(value = Loggable.DEBUG)
	public void destroyPigeon(String clientId, int port, String userName) {
		brokerAccessControllers.get(port).remove(this, clientId, userName);
	}

	@Loggable(value = Loggable.DEBUG)
	public void reloadConfiguration(int port, String reloadAction,
			String userName, String password) throws PigeonAgentException {
		brokerAccessControllers.get(port).reload(this, reloadAction, userName,
				password);
	}

	/*
	 * Add new connection to the associated queue in the map. If connection
	 * already exists then delete existing connection and add new one.
	 */
	@Loggable(value = Loggable.DEBUG)
	public void addToQueue(Map<Integer, DelayQueue<Connection>> map,
			String clientId, int port, int clientApiVersion) {
		Connection connection = new Connection(port, clientId,
				System.currentTimeMillis(), clientApiVersion);
		if (!map.containsKey(port)) {
			map.put(port, new DelayQueue<Connection>());
		}
		DelayQueue<Connection> dQueue = map.get(port);
		if (dQueue.contains(connection)) {
			dQueue.remove(connection);
		}
		dQueue.add(connection);
	}

	/*
	 * Add new clientId to the associated list in the map. If clientId already
	 * exists then delete existing clientId and add new one.
	 */
	@Loggable(value = Loggable.DEBUG)
	public void addToList(Map<Integer, List<String>> map, String clientId,
			int port) {
		if (!map.containsKey(port)) {
			map.put(port, Collections.synchronizedList(new ArrayList<String>()));
		}
		List<String> pigeons = map.get(port);
		if (!pigeons.contains(clientId)) {
			map.get(port).add(clientId);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public boolean mapContains(Map<Integer, DelayQueue<Connection>> map,
			String clientId, int port) {
		if (map.containsKey(port)) {
			return map.get(port).contains(clientId);
		}
		return false;
	}

	@Loggable(value = Loggable.DEBUG)
	public void removeFromQueue(Map<Integer, DelayQueue<Connection>> map,
			String clientId, int port) {
		if (map.containsKey(port)) {
			DelayQueue<Connection> connections = map.get(port);
			connections.remove(clientId);
			if (connections.isEmpty())
				map.remove(port);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void removeFromList(Map<Integer, List<String>> map, String clientId,
			int port) {
		if (map.containsKey(port)) {
			List<String> pigeons = map.get(port);
			pigeons.remove(clientId);
			if (pigeons.isEmpty()) {
				map.remove(port);
			}
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void generatePigeons() {
		try {
			pigeonServerUser.generateAgentPigeons(agent.getIp(),
					agent.getAgentName());
		} catch (PigeonAgentException e) {
			Logger.warn(this,
					"Exception occured in generatePigeons()  %[exception]s...",
					e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public synchronized void sendPigeonsToServer() {
		try {
			if (!pigeonsReadyToUse.isEmpty()) {
				pigeonServerUser.activatePigeons(pigeonsReadyToUse);
				pigeonsReadyToUse.clear();
			}
		} catch (PigeonAgentException e) {
			Logger.error(this, e.getMessage());
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public synchronized void deletePigeonsFromServer(int port) {
		try {
			if (!pigeonsDestroyed.isEmpty()) {
				pigeonServerUser.deletePigeons(pigeonsDestroyed);
				pigeonsDestroyed.clear();
			}
			destroyedPigeonsCount.put(port, 0);
		} catch (PigeonAgentException e) {
			Logger.error(this, e.getMessage());
		}
	}

	private void sleep(int count) {
		try {
			Thread.sleep(200 * count);
		} catch (InterruptedException e) {
			Logger.error(this, "Error in sleep");
		}
	}

	@Override
	public void onSuccessfullyAdded(String clientId, int port) {
		addToList(availablePigeonList, clientId, port);
		pigeonsReadyToUse.add(clientId);

		int count = getReadyToUsePigeonsCount().get(port);
		count++;
		getReadyToUsePigeonsCount().put(port, count);
	}

	@Override
	public void onSuccessfullyRemoved(String clientId, int port) {
		removeFromList(availablePigeonList, clientId, port);
		pigeonsDestroyed.add(clientId);

		int count = destroyedPigeonsCount.get(port);
		count++;
		destroyedPigeonsCount.put(port, count);
	}

	@Override
	public void onReloadCompletion(String reloadAction, int port) {
		if (reloadAction.equals(RELOAD_ACTION_ADD)) {
			sendPigeonsToServer();
		} else if (reloadAction.equals(RELOAD_ACTION_DELETE)) {
			deletePigeonsFromServer(port);
		} else {
			Logger.warn(this, "Invalid reload action");
		}
	}

	@Override
	public boolean verifyReload(String reloadAction, int port, String userName,
			String password) {
		if (reloadAction.equals(RELOAD_ACTION_ADD)) {
			return testConnection(port, userName, password);
		} else if (reloadAction.equals(RELOAD_ACTION_DELETE)) {
			return !testConnection(port, userName, password);
		}
		Logger.warn(this, "Invalid reload action");
		return true;
	}

	@Loggable(value = Loggable.DEBUG)
	private boolean testConnection(int port, String userName, String password) {
		try {
			MqttClient mqttClient = new MqttClient("ssl://"
					+ agentConfigurator.getHostIpAddress() + ":" + port,
					"testClientId", null);

			MqttConnectOptions options = new MqttConnectOptions();
			options.setUserName(userName);
			options.setPassword(password.toCharArray());
			options.setCleanSession(true);
			options.setSocketFactory(BrokerSSLConnector.getSocketFactory(
					agentConfigurator.getCaCertificateFile(),
					agentConfigurator.getClientCertificateFile(),
					agentConfigurator.getKeyFile(),
					agentConfigurator.getBrokerPassword(),
					agentConfigurator.getTlsVersion()));
			mqttClient.connect(options);
			if (mqttClient.isConnected()) {
				mqttClient.disconnect(1000);
				return true;
			}
		} catch (MqttException | PigeonAgentException | IOException
				| GeneralSecurityException e) {
		}
		return false;
	}

	// FIXME Must be done through BrokerAccessController only.
	private void addToPasswordFile(int port, String userName, String password)
			throws InterruptedException, IOException {
		String mosquittoPasswd = agentConfigurator
				.getMqttServerInstallationDir() + "mosquitto_passwd";
		String passwordFile = agentConfigurator.getAgentInstallationDir()
				+ "/config/pwfile_" + port + ".txt ";
		String command = mosquittoPasswd + " -b " + passwordFile + " "
				+ userName + " " + password;
		Runtime.getRuntime().exec(command).waitFor();
	}

	public void reallocateExpiredPigons() {
		for (int port : onlinePigeonList.keySet()) {
			DelayQueue<Connection> connections = onlinePigeonList.get(port);
			Connection connection;
			while ((connection = connections.poll()) != null) {
				try {
					Logger.info(this,
							"KeepAliveInterval passed. Reallocatin pigeon. ClientId: "
									+ connection.getClientId() + ", Port: "
									+ connection.getPort());
					reallocatePigeon(connection.getClientId(),
							connection.getPort(), false);
				} catch (PigeonAgentException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class Connection implements Delayed {

		private int port;

		private String clientId;

		private long lastPingTime;

		private int pigeonClientAPIVersion;

		public Connection(String clientId) {
			this.clientId = clientId;
		}

		public Connection(int port, String clientId, long lastPingTime,
				int pigeonClientAPIVersion) {
			this.port = port;
			this.clientId = clientId;
			this.lastPingTime = lastPingTime;
			this.pigeonClientAPIVersion = pigeonClientAPIVersion;
		}

		public int getPort() {
			return port;
		}

		public String getClientId() {
			return clientId;
		}

		@Override
		public long getDelay(TimeUnit timeUnit) {
			// Do not expire the connection if pigeonClientAPIVersion is 0. As
			// client API with version 0 will never send ping required by agent.
			if (pigeonClientAPIVersion == 0) {
				return 1;
			}

			long diff = lastPingTime + KEEP_ALIVE_INTERVAL
					- System.currentTimeMillis();
			return timeUnit.convert(diff, TimeUnit.MILLISECONDS);
		}

		@Override
		public int compareTo(Delayed o) {
			if (this.lastPingTime < ((Connection) o).lastPingTime) {
				return -1;
			}
			if (this.lastPingTime > ((Connection) o).lastPingTime) {
				return 1;
			}
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Connection) {
				return this.clientId.equals(((Connection) obj).clientId);
			}
			return false;
		}

		@Override
		public String toString() {
			return "Connection [clientId=" + clientId + ", lastPingTime="
					+ lastPingTime + "]";
		}
	}

	public List<Broker> getBridgedBrokers() {
		return bridgeBrokers ;
	}
}
