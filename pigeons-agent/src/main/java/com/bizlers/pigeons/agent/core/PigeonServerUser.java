package com.bizlers.pigeons.agent.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.bizlers.auth.client.Session;
import com.bizlers.pigeons.commommodels.Agent;
import com.bizlers.pigeons.commommodels.Broker;
import com.bizlers.pigeons.commommodels.Pigeon;
import com.bizlers.utils.jaxrs.SecuredClientBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

public class PigeonServerUser {

	// Methods to create instances of WebResource are thread-safe. Methods that
	// modify configuration and or
	// filters are not guaranteed to be thread-safe. The creation of a Client
	// instance is an expensive operation and the instance may make use of and
	// retain many resources. It is therefore recommended that a Client instance
	// is reused for the creation of WebResource instances that require the same
	// configuration settings.

	public static final String URL_AGENTS = "pigeons-server/rest/agents";

	public static final String URL_BROKERS = "pigeons-server/rest/brokers";

	public static final String URL_PIGEONS = "pigeons-server/rest/pigeons";

	private static Client client;

	private String pigeonServerName;

	private int pigeonServerPort;

	private String pigeonServerAddress;

	private AgentService agentService;

	public PigeonServerUser(AgentConfigurator config, Session session,
			AgentService agentService) throws GeneralSecurityException,
			IOException {
		this(config.getPigeonServerHostName(), config.getPigeonServerPort(),
				config.getKeyStorePath(), config.getTrustStorePath(), config
						.getKeyStorePassword(), config.getTrustStorePassword(),
				config.getPrivateKeyPassword(), String.valueOf(session
						.getAccountId()), session.getSessionId(), agentService);
	}

	@Loggable(value = Loggable.DEBUG)
	public PigeonServerUser(String pigeonServerName, int pigeonServerPort,
			String keyStorePath, String trustStorePath,
			char[] keyStorePassword, char[] trustStorePassword,
			char[] privateKeyPassword, String agentUserName,
			String agentPassword, AgentService agentService)
			throws GeneralSecurityException, IOException {
		this.pigeonServerName = pigeonServerName;
		this.pigeonServerPort = pigeonServerPort;
		this.pigeonServerAddress = "https://" + pigeonServerName + ":"
				+ pigeonServerPort + "/";
		this.agentService = agentService;
		client = new SecuredClientBuilder().keyStorePath(keyStorePath)
				.trustStorePath(trustStorePath)
				.keyStorePassword(String.valueOf(keyStorePassword))
				.trustStorePassword(String.valueOf(trustStorePassword))
				.privateKeyPassword(String.valueOf(privateKeyPassword)).build();
		client.addFilter(new HTTPBasicAuthFilter(agentUserName, agentPassword));
	}

	public String getPigeonServerName() {
		return pigeonServerName;
	}

	public void setPigeonServerName(String pigeonServerName) {
		this.pigeonServerName = pigeonServerName;
	}

	public int getPigeonServerPort() {
		return pigeonServerPort;
	}

	public void setPigeonServerPort(int pigeonServerPort) {
		this.pigeonServerPort = pigeonServerPort;
	}

	public String getPigeonServerAddress() {
		return pigeonServerAddress;
	}

	@Loggable(value = Loggable.DEBUG)
	public void registerBroker(Broker broker) throws PigeonAgentException {

		try {
			// Broker name will be generated and provided by Pigeon Server.
			broker.setBrokerName(null);
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_BROKERS);
			ClientResponse response = webResource.type(
					MediaType.APPLICATION_JSON).post(ClientResponse.class,
					broker);

			if (response.getStatus() != ClientResponse.Status.CREATED
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to register the broker - " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				Logger.info(this,
						"Sucessfully registered broker IP: %s Port: %d",
						broker.getIp(), broker.getPort());
				String brokerName = response.getEntity(String.class);
				broker.setBrokerName(brokerName);
				agentService.getBrokerList().put(broker.getPort(), brokerName);
				agentService.getReadyToUsePigeonsCount().put(broker.getPort(),
						0);
				agentService.getDestroyedPigeonsCount()
						.put(broker.getPort(), 0);
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException("Failed to register Broker  IP : "
					+ broker.getIp() + " Port : " + broker.getPort()
					+ " Region : " + broker.getRegion() + e.getMessage(), e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public Broker getBrokerByName(String brokerName)
			throws PigeonAgentException {
		Broker result = null;
		try {
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_BROKERS + "/" + brokerName);
			ClientResponse response = webResource.get(ClientResponse.class);
			if (response.getStatus() == ClientResponse.Status.NOT_FOUND
					.getStatusCode()) {
				return null;
			} else if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to retrieve the broker - " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				result = response.getEntity(Broker.class);
				Logger.info(this, "Sucessfully retrieved the broker %s ",
						result.getBrokerName());
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException("Failed to get Broker : "
					+ brokerName + e.getMessage(), e);
		}
		return result;
	}

	@Loggable(value = Loggable.TRACE)
	public Broker getBroker(String ip, int port) throws PigeonAgentException {
		Broker result = null;
		try {
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_BROKERS);
			MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
			queryParams.add("queryBroker", "true");
			queryParams.add("ip", ip);
			queryParams.add("port", String.valueOf(port));
			ClientResponse response = webResource.queryParams(queryParams).get(
					ClientResponse.class);
			if (response.getStatus() == ClientResponse.Status.NOT_FOUND
					.getStatusCode()) {
				return null;
			} else if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to retrieve the broker -  " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				result = response.getEntity(Broker.class);
				Logger.info(this, "Sucessfully retrieved the broker %s ",
						result.getBrokerName());
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException(
					"Failed to get Broker from Server with IP : " + ip
							+ " Port : " + port + e.getMessage(), e);
		}
		return result;

	}

	@Loggable(value = Loggable.DEBUG)
	public List<Broker> getBrokers(String type) throws PigeonAgentException {
		List<Broker> result = null;
		try {
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_BROKERS);

			MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
			queryParams.add("brokerType", type);
			queryParams.add("region", null);
			queryParams.add("list", "true");
			queryParams.add("status", Broker.STATUS_ONLINE);
			ClientResponse response = webResource.queryParams(queryParams).get(
					ClientResponse.class);
			if (response.getStatus() == ClientResponse.Status.NOT_FOUND
					.getStatusCode()) {
				return null;
			} else if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to retrieve the broker -  " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				try {
					InputStream inputStream = response.getEntityInputStream();
					result = new ObjectMapper().readValue(inputStream,
							new TypeReference<List<Broker>>() {
							});
					Logger.info(this,
							"Sucessfully retrieved the brokerList %s", result);
				} catch (IOException e) {
					throw new PigeonAgentException(
							"ERROR: Failed to retrieve the brokers from server.",
							e);
				}
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException(
					"Failed to get Brokers from Server with Type : " + type
							+ e.getMessage(), e);
		}
		return result;
	}

	@Loggable(value = Loggable.DEBUG)
	public Broker getBroker(String type, String region)
			throws PigeonAgentException {
		Broker result = null;
		try {
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_BROKERS);
			MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
			queryParams.add("brokerType", type);
			queryParams.add("region", region);
			queryParams.add("status", Broker.STATUS_ONLINE);
			ClientResponse response = webResource.queryParams(queryParams).get(
					ClientResponse.class);
			if (response.getStatus() == ClientResponse.Status.NOT_FOUND
					.getStatusCode()) {
				return null;
			} else if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to retrieve the broker -  " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				result = response.getEntity(Broker.class);
				Logger.info(this, "Sucessfully retrieved the broker %s ",
						result.getBrokerName());
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException(
					"Failed to get Broker from Server with Type : " + type
							+ " Region : " + region + e.getMessage(), e);
		}
		return result;
	}

	@Loggable(value = Loggable.DEBUG)
	public void updateBroker(Broker broker) throws PigeonAgentException {
		try {
			if (broker.getBrokerName() == null) {
				Logger.info(this, "ERROR: Broker name cannot be null");
			} else {
				WebResource webResource = client.resource(pigeonServerAddress
						+ URL_BROKERS + "/" + broker.getBrokerName());
				ClientResponse response = webResource.type(
						MediaType.APPLICATION_JSON).put(ClientResponse.class,
						broker);
				if (response.getStatus() != ClientResponse.Status.OK
						.getStatusCode()) {
					String errorMsg = response.getStatusInfo()
							.getReasonPhrase();
					throw new PigeonAgentException(
							"ERROR: Failed to update the broker - " + errorMsg
									+ ". HTTP error code : "
									+ response.getStatus());
				} else {
					Logger.info(this, "Sucessfully updated broker %s",
							broker.getBrokerName());
					agentService.getBrokerList().put(broker.getPort(),
							broker.getBrokerName());
					agentService.getReadyToUsePigeonsCount().put(
							broker.getPort(), 0);
					agentService.getDestroyedPigeonsCount().put(
							broker.getPort(), 0);

				}
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException("Failed to update Broker : "
					+ broker.getBrokerName() + e.getMessage(), e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deleteBroker(String brokerName) throws PigeonAgentException {
		try {
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_BROKERS + "/" + brokerName);
			ClientResponse response = webResource.delete(ClientResponse.class);

			if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to delete the broker - " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				Logger.info(this, "Sucessfully deleted the broker %s",
						brokerName);
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException("Failed to delete broker : "
					+ brokerName + e.getMessage(), e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void registerAgent(Agent agent) throws PigeonAgentException {
		try {
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_AGENTS);
			ClientResponse response = webResource.type(
					MediaType.APPLICATION_JSON).post(ClientResponse.class,
					agent);

			if (response.getStatus() != ClientResponse.Status.CREATED
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to register the agent - " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				agent.setAgentId(response.getEntity(Long.class));
				Logger.info(this, "Sucessfully registered agent %s",
						agent.getAgentName());
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException("Failed to register the Agent."
					+ e.getMessage(), e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public Agent getAgent(String ipAddess, String agentName)
			throws PigeonAgentException {
		Agent result = null;
		try {
			String agentId = agentName + ":" + ipAddess;
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_AGENTS + "/" + agentId);

			ClientResponse response = webResource.get(ClientResponse.class);
			if (response.getStatus() == ClientResponse.Status.NOT_FOUND
					.getStatusCode()) {
				return null;
			} else if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to retrieve the agent -  " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				result = response.getEntity(Agent.class);
				Logger.info(this, "Sucessfully retrieved the agent %s",
						result.getAgentName());
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException("Failed to get Agent."
					+ e.getMessage(), e);
		}
		return result;
	}

	@Loggable(value = Loggable.DEBUG)
	public void generateAgentPigeons(String ipAddess, String agentName)
			throws PigeonAgentException {
		try {
			String agentId = agentName + ":" + ipAddess;
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_AGENTS + "/" + agentId + "/generate");

			ClientResponse response = webResource.get(ClientResponse.class);
			if (response.getStatus() == ClientResponse.Status.NOT_FOUND
					.getStatusCode()) {
			} else if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to retrieve the agent -  " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				Logger.info(this,
						"Sucessfully sent generation call for pigeons ");
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException(
					"Call Failed to generate Agent Pigeons." + e.getMessage(),
					e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void updateAgent(Agent agent) throws PigeonAgentException {
		try {
			if (agent.getAgentName() == null || agent.getIp() == null) {
				Logger.info(this,
						"ERROR: Agent name or agent ip address cannot be null");
			} else {
				String agentId = agent.getAgentName() + ":" + agent.getIp();
				WebResource webResource = client.resource(pigeonServerAddress
						+ URL_AGENTS + "/" + agentId);
				ClientResponse response = webResource.type(
						MediaType.APPLICATION_JSON).put(ClientResponse.class,
						agent);
				if (response.getStatus() != ClientResponse.Status.OK
						.getStatusCode()) {
					String errorMsg = response.getStatusInfo()
							.getReasonPhrase();
					throw new PigeonAgentException(
							"ERROR: Failed to update the agent - " + errorMsg
									+ ". HTTP error code : "
									+ response.getStatus());
				} else {
					Logger.info(this, "Sucessfully updated agent %s",
							agent.getAgentName());
				}
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException("Failed to update Agent."
					+ e.getMessage(), e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deleteAgent(String ipAddess, String agentName)
			throws PigeonAgentException {
		try {
			String agentId = agentName + ":" + ipAddess;
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_AGENTS + "/" + agentId);
			ClientResponse response = webResource.delete(ClientResponse.class);
			if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to delete the agent -  " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				Logger.info(this,
						"Sucessfully deleted the Agent IP :%s ,Name : %s "
								+ ipAddess, agentName);
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException("Failed to delete Agent."
					+ e.getMessage(), e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPigeon(String clientId) throws PigeonAgentException {
		return getPigeonByClientId(clientId);
	}

	@Loggable(value = Loggable.DEBUG)
	private Pigeon getPigeonByClientId(String clientId)
			throws PigeonAgentException {
		Pigeon result = null;
		try {
			clientId = clientId.replaceAll("/", ":");
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_PIGEONS + "/" + clientId);
			MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
			ClientResponse response = webResource.queryParams(queryParams).get(
					ClientResponse.class);
			if (response.getStatus() == ClientResponse.Status.NOT_FOUND
					.getStatusCode()) {
				return null;
			} else if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to retrieve the pigeon - " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				result = response.getEntity(Pigeon.class);
				Logger.info(this, "Sucessfully retrieved the pigeon %s",
						result.getClientId());
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException(
					"Failed to get pigeon with ClientId : " + clientId
							+ e.getMessage(), e);
		}
		return result;
	}

	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPigeonByRegion(String appId, String region)
			throws PigeonAgentException {
		Pigeon result = null;
		try {
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_PIGEONS);
			MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
			queryParams.add("region", region);
			queryParams.add("appId", appId);
			ClientResponse response = webResource.queryParams(queryParams).get(
					ClientResponse.class);
			if (response.getStatus() == ClientResponse.Status.NOT_FOUND
					.getStatusCode()) {
				return null;
			} else if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to retrieve the pigeon -  " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				result = response.getEntity(Pigeon.class);
				Logger.info(this, "Sucessfully retrieved the pigeon %s",
						result.getClientId());
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException("Failed to get pigeon. "
					+ e.getMessage(), e);
		}
		return result;
	}

	@Loggable(value = Loggable.DEBUG)
	public void activatePigeons(List<String> pigeons)
			throws PigeonAgentException {
		try {
			Logger.debug(this, "Sending pigeons for activation.");
			if (pigeons == null || pigeons.isEmpty()) {
				Logger.debug(this, "No pigeons to activate.");
				return;
			}

			String fileName = "TMP_ACTIVATE_"
					+ Long.toHexString(Double.doubleToLongBits(Math.random()))
					+ ".txt";
			FileWriter fstream = new FileWriter(fileName, false);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(pigeons.toString());
			out.close();

			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_PIGEONS + "/activate");

			File file = new File(fileName);
			FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file",
					file);
			FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
			formDataMultiPart.bodyPart(fileDataBodyPart);
			ClientResponse response = webResource.type(
					MediaType.MULTIPART_FORM_DATA).put(ClientResponse.class,
					formDataMultiPart);
			file.delete();
			if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to activate the pigeons -  " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				Logger.info(this, "Sucessfully activated the pigeons ");
			}
		} catch (UniformInterfaceException | ClientHandlerException
				| IOException e) {
			throw new PigeonAgentException("Failed to activate the pigeons. "
					+ e.getMessage(), e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deletePigeons(List<String> pigeons) throws PigeonAgentException {
		try {
			Logger.debug(this, "Deleting pigeons form server.");
			if (pigeons == null || pigeons.isEmpty()) {
				Logger.debug(this, "No pigeons to delete.");
				return;
			}

			String fileName = "TMP_DELETE_"
					+ Long.toHexString(Double.doubleToLongBits(Math.random()))
					+ ".txt";
			FileWriter fstream = new FileWriter(fileName, false);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(pigeons.toString());
			out.close();

			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_PIGEONS + "/delete");

			File file = new File(fileName);
			FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file",
					file);
			FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
			formDataMultiPart.bodyPart(fileDataBodyPart);
			ClientResponse response = webResource.type(
					MediaType.MULTIPART_FORM_DATA).put(ClientResponse.class,
					formDataMultiPart);
			file.delete();
			if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to delete the pigeons -  " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				Logger.info(this,
						"Sucessfully sent request to delete the pigeons ");
			}
		} catch (UniformInterfaceException | ClientHandlerException
				| IOException e) {
			throw new PigeonAgentException("Failed to delete the pigeons. "
					+ e.getMessage(), e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void updatePigeon(Pigeon pigeon, boolean isPendingCall)
			throws PigeonAgentException {
		try {
			if (pigeon.getClientId() == null) {
				Logger.error(this, "Client ID for pigeon cannot be null");
			} else {
				String clientId = pigeon.getClientId().replaceAll("/", ":");
				WebResource webResource = client.resource(pigeonServerAddress
						+ URL_PIGEONS + "/" + clientId);
				ClientResponse response = webResource.type(
						MediaType.APPLICATION_JSON).put(ClientResponse.class,
						pigeon);
				if (response.getStatus() != ClientResponse.Status.OK
						.getStatusCode()) {
					String errorMsg = response.getStatusInfo()
							.getReasonPhrase();
					throw new PigeonAgentException(
							"ERROR: Failed to update the pigeon - " + errorMsg
									+ ". HTTP error code : "
									+ response.getStatus());
				} else {
					Logger.debug(this,
							"Sucessfully updated pigeon %s Pending %s",
							pigeon.getClientId(), isPendingCall);
				}
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException(
					"Failed to update  pigeon with ClientId : "
							+ pigeon.getClientId() + e.getMessage(), e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deletePigeon(String clientId) throws PigeonAgentException {
		try {
			clientId = clientId.replaceAll("/", ":");
			WebResource webResource = client.resource(pigeonServerAddress
					+ URL_PIGEONS + "/" + clientId);
			ClientResponse response = webResource.delete(ClientResponse.class);
			if (response.getStatus() == ClientResponse.Status.NOT_FOUND
					.getStatusCode()) {
				Logger.warn(this, "Pigeon not found for deletion %s ", clientId);
			} else if (response.getStatus() != ClientResponse.Status.OK
					.getStatusCode()) {
				String errorMsg = response.getStatusInfo().getReasonPhrase();
				throw new PigeonAgentException(
						"ERROR: Failed to delete the pigeon - " + errorMsg
								+ ". HTTP error code : " + response.getStatus());
			} else {
				Logger.info(this, "Sucessfully deleted the pigeon %s ",
						clientId);
			}
		} catch (UniformInterfaceException | ClientHandlerException e) {
			throw new PigeonAgentException(
					"Failed to delete the pigeon with ClientId :  " + clientId
							+ e.getMessage(), e);
		}
	}
}
