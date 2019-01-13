package com.bizlers.pigeons.agent.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

import com.bizlers.pigeons.commommodels.Broker;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

public class AgentConfigurator {

	private PropertiesConfiguration config;
	private String configFilePath;
	private AgentService agentService;

	private List<Integer> brokerPortArrayListL0 = new ArrayList<Integer>();
	private List<Integer> brokerPortArrayListL1 = new ArrayList<Integer>();
	private List<Integer> brokerPortArrayListL2 = new ArrayList<Integer>();

	public static final String HOSTNAME = "host_name";
	public static final String HOST_IP_ADDRESS = "host_ip_address";
	public static final String REGION = "region";
	public static final String AGENT_NAME = "agent_name";
	public static final String AGENT_INSTALLATION_DIR = "agent_installation_dir";
	public static final String PASSWORD_FILE = "password_file";
	public static final String STATUS_LISTENER_BROKER_PORT = "status_listener_broker_port";
	public static final String MQTT_SERVER_INSTALLATION_DIR = "mqtt_server_installation_dir";
	public static final String PIGEON_SERVER_HOSTNAME = "pigeon_server_hostname";
	public static final String PIGEON_SERVER_PORT = "pigeon_server_port";
	public static final String START_AND_MONITOR_L0 = "start_and_monitor_l0";
	public static final String START_AND_MONITOR_L1 = "start_and_monitor_l1";
	public static final String START_AND_MONITOR_L2 = "start_and_monitor_l2";
	public static final String NUMBER_OF_BROKERS_L0 = "number_of_brokers_l0";
	public static final String NUMBER_OF_BROKERS_L1 = "number_of_brokers_l1";
	public static final String NUMBER_OF_BROKERS_L2 = "number_of_brokers_l2";
	public static final String BROKER_PORT_LIST_L0 = "broker_port_list_l0";
	public static final String BROKER_PORT_LIST_L1 = "broker_port_list_l1";
	public static final String BROKER_PORT_LIST_L2 = "broker_port_list_l2";
	public static final String KEYSTORE_PATH = "keystore_path";
	public static final String KEYSTORE_PASSWORD = "keystore_password";
	public static final String TRUSTSTORE_PATH = "truststore_path";
	public static final String TRUSTSTORE_PASSWORD = "truststore_password";
	public static final String PRIVATE_KEY_PASSWORD = "private_key_password";
	public static final String AUTH_SERVER_URL = "auth_service_url";
	public static final String APP_NAME = "app_name";
	public static final String DEVICE_ID = "device_id";
	public static final String AGENT_USERNAME = "agent_username";
	public static final String AGENT_PASSWORD = "agent_password";
	public static final String BROKER_PASSWORD = "broker_password";
	public static final String CLIENT_CERTIFICATE_FILE = "client_certificate_file";
	public static final String CA_CERTIFICATE_FILE = "ca_certificate_file";
	public static final String KEY_FILE = "key_file";
	private static final String TLS_VERSION = "tls_version";
	private static final String BRIDGE_TLS_VERSION = "bridge_tls_version";
	private static final String REDIRECT_DESTINATION_PORT = "redirect_destination_port";
	private static final String REDIRECT_PORT = "redirect_port";
	private static final String USER = "user";
	public static final int MAX_RETRY_COUNT = 5;

	public AgentConfigurator(AgentService agentService) {
		this.agentService = agentService;
	}

	@Loggable(value = Loggable.DEBUG)
	public void loadAgentConfiguration() throws PigeonAgentException {
		try {
			Logger.info(this, "Configuration File : '%s'", configFilePath);
			File configFile = new File(configFilePath);
			config = new PropertiesConfiguration(configFile);
			config.setReloadingStrategy(new FileChangedReloadingStrategy());

			if (isStartL0())
				brokerPortArrayListL0 = getPortList(getBorkerPortListL0());
			if (isStartL1())
				brokerPortArrayListL1 = getPortList(getBorkerPortListL1());
			if (isStartL2())
				brokerPortArrayListL2 = getPortList(getBorkerPortListL2());
		} catch (ConfigurationException e) {
			Logger.error(this,
					"Failed to load agent configuration. %[exception]s", e);
			throw new PigeonAgentException(
					"Failed to load agent configuration.");
		}
	}

	public String getConfigFilePath() {
		return configFilePath;
	}

	public void setConfigFilePath(String configFilePath) {
		this.configFilePath = configFilePath;
	}

	public String getUser() {
		return config.getString(USER);
	}

	public String getHostName() {
		return config.getString(HOSTNAME);
	}

	public String getHostIpAddress() {
		return config.getString(HOST_IP_ADDRESS);
	}

	public String getRegion() {
		return config.getString(REGION);
	}

	public String getAgentName() {
		return config.getString(AGENT_NAME);
	}

	public String getAgentUserName() {
		return config.getString(AGENT_USERNAME);
	}

	public String getAgentPassword() {
		return config.getString(AGENT_PASSWORD);
	}

	public String getAgentInstallationDir() {
		return config.getString(AGENT_INSTALLATION_DIR);
	}

	public String getPasswordFilePath() {
		return config.getString(PASSWORD_FILE);
	}

	public int getStatusListenerBrokerPort() {
		return config.getInt(STATUS_LISTENER_BROKER_PORT);
	}

	public String getMqttServerInstallationDir() {
		String dir = config.getString(MQTT_SERVER_INSTALLATION_DIR);
		if(dir != null) {
			return dir + File.separator;
		}
		return "";
	}

	public String getPigeonServerHostName() {
		return config.getString(PIGEON_SERVER_HOSTNAME);
	}

	public String getAuthServerUrl() {
		return config.getString(AUTH_SERVER_URL);
	}

	public String getAppName() {
		return config.getString(APP_NAME);
	}

	public String getDeviceId() {
		return config.getString(DEVICE_ID);
	}

	public int getPigeonServerPort() {
		return config.getInt(PIGEON_SERVER_PORT);
	}

	public boolean isStartL0() {
		return config.getBoolean(START_AND_MONITOR_L0);
	}

	public boolean isStartL1() {
		return config.getBoolean(START_AND_MONITOR_L1);
	}

	public boolean isStartL2() {
		return config.getBoolean(START_AND_MONITOR_L2);
	}

	public int getNumberOfBrokersL1() {
		return config.getInt(NUMBER_OF_BROKERS_L1);
	}

	public int getNumberOfBrokersL2() {
		return config.getInt(NUMBER_OF_BROKERS_L2);
	}

	public boolean isRedirectPortAvailable() {
		try {
			config.getInt(REDIRECT_PORT);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public boolean isRedirectDestinationPortAvailable() {
		try {
			config.getInt(REDIRECT_DESTINATION_PORT);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public int getRedirectPort() {
		return config.getInt(REDIRECT_PORT);
	}

	public int getRedirectDestinationPort() {
		return config.getInt(REDIRECT_DESTINATION_PORT);
	}

	public String getBorkerPortListL0() {
		return config.getString(BROKER_PORT_LIST_L0);
	}

	public String getBorkerPortListL1() {
		return config.getString(BROKER_PORT_LIST_L1);
	}

	public String getBorkerPortListL2() {
		return config.getString(BROKER_PORT_LIST_L2);
	}

	public List<Integer> getBrokerPortArrayListL0() {
		return brokerPortArrayListL0;
	}

	public List<Integer> getBrokerPortArrayListL1() {
		return brokerPortArrayListL1;
	}

	public List<Integer> getBrokerPortArrayListL2() {
		return brokerPortArrayListL2;
	}

	public String getKeyStorePath() {
		return config.getString(KEYSTORE_PATH);
	}

	public char[] getKeyStorePassword() {
		return config.getString(KEYSTORE_PASSWORD).toCharArray();
	}

	public String getTrustStorePath() {
		return config.getString(TRUSTSTORE_PATH);
	}

	public String getClientCertificateFile() {
		return config.getString(CLIENT_CERTIFICATE_FILE);
	}

	public String getCaCertificateFile() {
		return config.getString(CA_CERTIFICATE_FILE);
	}

	public String getKeyFile() {
		return config.getString(KEY_FILE);
	}

	public String getBrokerPassword() {
		return config.getString(BROKER_PASSWORD);
	}

	public String getTlsVersion() {
		return config.getString(TLS_VERSION);
	}

	public String getBridgeTlsVersion() {
		return config.getString(BRIDGE_TLS_VERSION);
	}

	public char[] getTrustStorePassword() {
		return config.getString(TRUSTSTORE_PASSWORD).toCharArray();
	}

	public char[] getPrivateKeyPassword() {
		return config.getString(PRIVATE_KEY_PASSWORD).toCharArray();
	}

	@Loggable(value = Loggable.TRACE)
	public void addBridgeConfiguration(BufferedWriter out) throws IOException {
		out.newLine();
		out.newLine();
		out.write(MqttConnector.MQTT_BRIDGE_TLS_VERSION + " "
				+ getBridgeTlsVersion());
		out.newLine();
		out.write(MqttConnector.MQTT_CONFIG_BRIDGE_CA_CERTIFICATE_FILE + " "
				+ getCaCertificateFile());
		out.newLine();
		out.write(MqttConnector.MQTT_CONFIG_BRIDGE_CERTIFICATE_FILE + " "
				+ getClientCertificateFile());
		out.newLine();
		out.write(MqttConnector.MQTT_CONFIG_BRIDGE_KEYFILE + " " + getKeyFile());
		out.newLine();
		out.write(MqttConnector.MQTT_USERNAME + " "
				+ MqttConnector.MQTT_BRIDGE_USERNAME);
		out.newLine();
		out.write(MqttConnector.MQTT_PASSWORD + " "
				+ MqttConnector.MQTT_BRIDGE_PASSWORD);
		out.newLine();
		out.newLine();

	}

	@Loggable(value = Loggable.DEBUG)
	public void createDefaultConfigurationFile(Broker currentBroker,
			Broker parentBroker, List<Broker> brokerList) throws IOException,
			PigeonAgentException {
		InputStream inStream = null;
		OutputStream outStream = null;

		File source = new File(getAgentInstallationDir()
				+ "/config/defaultConfig.conf");
		File dest = new File(getAgentInstallationDir() + "/config/config_"
				+ currentBroker.getPort() + ".conf");

		inStream = new FileInputStream(source);
		outStream = new FileOutputStream(dest);

		byte[] buffer = new byte[1024];

		int length;
		while ((length = inStream.read(buffer)) > 0) {
			outStream.write(buffer, 0, length);
		}

		inStream.close();
		outStream.close();

		FileWriter fileWriter = new FileWriter(dest, true);
		BufferedWriter out = new BufferedWriter(fileWriter);
		out.newLine();
		out.write(MqttConnector.MQTT_CONFIG_BIND_ADDRESS + " "
				+ getHostIpAddress());
		out.newLine();
		out.write(MqttConnector.MQTT_CONFIG_PORT + " "
				+ currentBroker.getPort());
		out.newLine();

		String passwordFilePath = getAgentInstallationDir() + "/config/pwfile_"
				+ currentBroker.getPort() + ".txt";
		File file = new File(passwordFilePath);
		file.createNewFile();
		out.write(MqttConnector.MQTT_CONFIG_PASSWORD_FILE + " "
				+ passwordFilePath);
		out.newLine();
		String localdbFilePath = getAgentInstallationDir() + "/config/";
		out.write(MqttConnector.MQTT_PERSISTENCE + " true");
		out.newLine();
		out.write(MqttConnector.MQTT_PERSISTENCE_LOCATION + " "
				+ localdbFilePath);
		out.newLine();
		out.write(MqttConnector.MQTT_PERSISTENCE_FILE + " " + "brokerstore_"
				+ currentBroker.getPort() + ".db");
		out.newLine();
		if (brokerList != null) {
			for (int i = 0; i < brokerList.size(); i++) {
				out.newLine();
				out.write(MqttConnector.MQTT_CONFIG_CONNECTION + " "
						+ currentBroker.getBrokerName() + "-"
						+ brokerList.get(i).getBrokerName());
				out.newLine();
				out.write(MqttConnector.MQTT_CONFIG_ADDRESS + " "
						+ brokerList.get(i).getIp() + ":"
						+ brokerList.get(i).getPort());
				out.newLine();
				if (currentBroker.getBrokerType().equalsIgnoreCase(
						Broker.BROKER_TYPE_LEVEL_L0)) {
					out.write(MqttConnector.MQTT_CONFIG_TOPIC + " USER/"
							+ brokerList.get(i).getBrokerName() + "/"
							+ "# out 1");
					out.newLine();
					out.write(MqttConnector.MQTT_CONFIG_TOPIC
							+ " USER/BROADCAST/# out 1");
					addBridgeConfiguration(out);
					out.write(MqttConnector.MQTT_CONFIG_CONNECTION + " "
							+ "ACK-" + currentBroker.getBrokerName() + "-"
							+ brokerList.get(i).getBrokerName());
					out.newLine();
					out.write(MqttConnector.MQTT_CONFIG_ADDRESS + " "
							+ brokerList.get(i).getIp() + ":"
							+ brokerList.get(i).getPort());
					out.newLine();

					out.write(MqttConnector.MQTT_CONFIG_TOPIC + " USER/ACK/"
							+ currentBroker.getBrokerName() + "/" + "# in 1");

				} else {
					out.write(MqttConnector.MQTT_CONFIG_TOPIC + " USER/"
							+ currentBroker.getBrokerName() + "/" + "# in 1");
					out.newLine();
					out.write(MqttConnector.MQTT_CONFIG_TOPIC
							+ " USER/BROADCAST/# in 1");
					addBridgeConfiguration(out);
					out.write(MqttConnector.MQTT_CONFIG_CONNECTION + " "
							+ "ACK-" + currentBroker.getBrokerName() + "-"
							+ brokerList.get(i).getBrokerName());
					out.newLine();
					out.write(MqttConnector.MQTT_CONFIG_ADDRESS + " "
							+ brokerList.get(i).getIp() + ":"
							+ brokerList.get(i).getPort());
					out.newLine();

					out.write(MqttConnector.MQTT_CONFIG_TOPIC + " USER/ACK/"
							+ brokerList.get(i).getBrokerName() + "/"
							+ "# out 1");
				}
				addBridgeConfiguration(out);

			}
		}
		if (parentBroker != null) {
			out.newLine();
			out.write(MqttConnector.MQTT_CONFIG_CONNECTION + " "
					+ currentBroker.getBrokerName() + "-"
					+ parentBroker.getBrokerName());
			out.newLine();
			out.write(MqttConnector.MQTT_CONFIG_ADDRESS + " "
					+ parentBroker.getIp() + ":" + parentBroker.getPort());
			out.newLine();
			if (currentBroker.getBrokerType().equalsIgnoreCase(
					Broker.BROKER_TYPE_LEVEL_L2)) {
				out.write(MqttConnector.MQTT_CONFIG_TOPIC + " USER/"
						+ parentBroker.getBrokerName() + "/"
						+ currentBroker.getBrokerName() + "/" + "# in 1");
				out.newLine();
				out.write(MqttConnector.MQTT_CONFIG_TOPIC
						+ " USER/BROADCAST/# in 1");
				addBridgeConfiguration(out);
				out.write(MqttConnector.MQTT_CONFIG_CONNECTION + " " + "ACK-"
						+ currentBroker.getBrokerName() + "-"
						+ parentBroker.getBrokerName());
				out.newLine();
				out.write(MqttConnector.MQTT_CONFIG_ADDRESS + " "
						+ parentBroker.getIp() + ":" + parentBroker.getPort());
				out.newLine();

				out.write(MqttConnector.MQTT_CONFIG_TOPIC + " USER/ACK/# out 1");

			} else {
				out.write(MqttConnector.MQTT_CONFIG_TOPIC + " USER/"
						+ currentBroker.getBrokerName() + "/" + "# in 1");
				out.newLine();
				out.write(MqttConnector.MQTT_CONFIG_TOPIC
						+ " USER/BROADCAST/# in 1");
				addBridgeConfiguration(out);
				out.write(MqttConnector.MQTT_CONFIG_CONNECTION + " " + "ACK-"
						+ currentBroker.getBrokerName() + "-"
						+ parentBroker.getBrokerName());
				out.newLine();
				out.write(MqttConnector.MQTT_CONFIG_ADDRESS + " "
						+ parentBroker.getIp() + ":" + parentBroker.getPort());
				out.newLine();

				out.write(MqttConnector.MQTT_CONFIG_TOPIC + " USER/ACK/# out 1");
			}
			addBridgeConfiguration(out);

		}

		if (agentService.getBrokerStatusListener() != null
				&& !(currentBroker.getBrokerType()
						.equalsIgnoreCase(Broker.BROKER_TYPE_LEVEL_L1))
				&& !(currentBroker.getBrokerType()
						.equalsIgnoreCase(Broker.BROKER_TYPE_STATUS_LISTENER))) {
			out.newLine();
			out.write(MqttConnector.MQTT_CONFIG_CONNECTION + " "
					+ currentBroker.getBrokerName() + "-"
					+ agentService.getBrokerStatusListener().getBrokerName());
			out.newLine();
			out.write(MqttConnector.MQTT_CONFIG_ADDRESS + " "
					+ getHostIpAddress() + ":"
					+ agentService.getBrokerStatusListener().getPort());
			out.newLine();
			out.write(MqttConnector.MQTT_CONFIG_TOPIC + " "
					+ MqttConnector.TOPIC_PIGEON_STATUS + " out 1");
			out.newLine();
			out.write(MqttConnector.MQTT_CONFIG_TOPIC + " "
					+ MqttConnector.TOPIC_PING + " out 1");
			out.newLine();
			out.write(MqttConnector.MQTT_CONFIG_TOPIC + " "
					+ MqttConnector.TOPIC_PIGEON_MIRROR + "# in 1");
			addBridgeConfiguration(out);

		}
		out.close();
		fileWriter.close();

		// Generate password files
		(new File(getAgentInstallationDir() + "/config/pwfile_"
				+ currentBroker.getPort() + ".txt")).createNewFile();
	}

	@Loggable(value = Loggable.TRACE)
	private ArrayList<Integer> getPortList(String value) {
		String[] list = new String[1024];
		List<Integer> portList = new ArrayList<Integer>();
		list = value.split(",");
		for (int i = 0; i < list.length; i++) {
			if (list[i].contains("-")) {
				int start = Integer.parseInt(list[i].substring(0,
						list[i].indexOf("-")).trim());
				int end = Integer.parseInt(list[i].substring(
						list[i].indexOf("-") + 1, list[i].length()).trim());
				for (int j = start; j <= end; j++) {
					portList.add(j);
				}
			} else
				portList.add(Integer.parseInt(list[i].trim()));
		}
		return (ArrayList<Integer>) portList;
	}
}
