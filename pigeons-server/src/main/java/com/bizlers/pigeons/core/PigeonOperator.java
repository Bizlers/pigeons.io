package com.bizlers.pigeons.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bizlers.pigeons.dao.PigeonDAO;
import com.bizlers.pigeons.models.Broker;
import com.bizlers.pigeons.models.ConnectionMessage;
import com.bizlers.pigeons.models.Pigeon;
import com.bizlers.pigeons.utils.ErrorState;
import com.bizlers.pigeons.utils.Generator;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

/**
 * @author saurabh
 * 
 */
@Service
public class PigeonOperator {

	private static boolean publishPigeonGeneration = false;
	private static boolean pigeonGeneration = false;
	
	@Autowired
	private PigeonDistributer pigeonDistributor;
	
	@Autowired
	private PigeonNotifier pigeonNotifier;
	
	@Autowired
	private BrokerOperator brokerOperator;
	
	@Autowired
	private PigeonGenerator pigeonGenerator;
	
	@Autowired
	private PigeonDAO pigeonDAOImp;

	@Loggable(value = Loggable.DEBUG)
	private boolean isValidatePigeon(Pigeon pigeon)
			throws PigeonServerException {
		int unique = 0, counter = 0;
		String userPassword = null;
		boolean flag = false;
		while (unique == 0) {
			pigeon.setClientId(pigeon.getClientId() + "/"
					+ Generator.getClientId());
			if (pigeonDAOImp.getPigeonbyClientId(pigeon.getClientId()) == null) {
				unique = 1;
				flag = true;
			} else {
				counter++;
			}
			if (counter == Pigeon.RETRYCOUNT) {
				unique = 1;
				throw new PigeonServerException(
						ErrorState.MAX_RETRY.getMessage());
			}
		}
		if (flag) {
			userPassword = Generator.getUserNamePassword();
			pigeon.setUserName(userPassword.substring(0, 44));
			pigeon.setPassword(userPassword.substring(45));

		}
		return flag;
	}

	@Loggable(value = Loggable.DEBUG)
	public void createPigeon(Pigeon pigeon) throws PigeonServerException {
		if (isValidatePigeon(pigeon)) {
			pigeon.setStatus(Pigeon.STATUS_CREATED);
			pigeonDAOImp.createPigeon(pigeon);
		} else {
			throw new PigeonServerException(ErrorState.FAILURE.getMessage());
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPublishPigeon(long appId) throws PigeonServerException {
		return getPublishPigeon(appId, null);
	}

	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPublishPigeonByBroker(long appId, String brokerName)
			throws PigeonServerException {
		return pigeonDAOImp.getPublishPigeons(appId, brokerName);
	}

	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPublishPigeon(long appId, String region)
			throws PigeonServerException {
		Pigeon pigeon = null;
		if (region == null || region.isEmpty()) {
			pigeon = pigeonDAOImp.getPublishPigeon(appId);
		} else {
			pigeon = pigeonDAOImp.getPublishPigeon(appId, region);
		}
		if (pigeon == null) {
			new GeneratePigeons(true, region, Pigeon.TYPE_L0).start();
			if (region == null || region.isEmpty()) {
				pigeon = pigeonDAOImp.getPublishPigeon(appId);
			} else {
				pigeon = pigeonDAOImp.getPublishPigeon(appId, region);
			}
		}
		if (pigeon != null) {
			brokerOperator.updateBroker(pigeon.getBrokerName(), 1, 0);
			if (pigeonDAOImp.getPigeonCount(pigeon.getBrokerName()) <= (Pigeon.PIGEONCOUNT / 3)) {
				Logger.info(
						this,
						"Pigeon Generation initiated as Pigeons are low of type : %s",
						Pigeon.TYPE_L0);
				new GeneratePigeons(true, region, Pigeon.TYPE_L0).start();
			}
		}
		return pigeon;
	}

	@Loggable(value = Loggable.DEBUG)
	public void activatePigeons(List<String> clientList) {
		new ActivatePigeons(clientList).start();
	}

	@Loggable(value = Loggable.DEBUG)
	public int getPigeonCount(String type) {
		return pigeonDAOImp.getPigeonCount(type);
	}

	@Loggable(value = Loggable.DEBUG)
	public Map<Long, List<Pigeon>> getPigeonsReadyToDistribute(String type) {
		Map<Long, List<Pigeon>> map = null;
		List<Pigeon> pigeons = pigeonDAOImp.getCreatedPigeons(type);
		if (pigeons != null) {
			map = new HashMap<Long, List<Pigeon>>();
			for (Pigeon pigeon : pigeons) {
				long agentId = pigeon.getAgentId();
				if (!map.containsKey(agentId)) {
					map.put(agentId, new ArrayList<Pigeon>());
				}
				map.get(agentId).add(pigeon);
			}
		}
		return map;
	}

	@Loggable(value = Loggable.DEBUG)
	public Map<Long, List<Pigeon>> getPigeonsReadyToDestroy(int idleTime) {
		Map<Long, List<Pigeon>> map = null;
		List<Pigeon> pigeons = pigeonDAOImp.getPigeonsToDestroy(idleTime);
		if (pigeons != null) {
			map = new HashMap<Long, List<Pigeon>>();
			for (Pigeon pigeon : pigeons) {
				long agentId = pigeon.getAgentId();
				if (!map.containsKey(agentId)) {
					map.put(agentId, new ArrayList<Pigeon>());
				}
				map.get(agentId).add(pigeon);
			}
		}
		return map;
	}

	@Loggable(value = Loggable.DEBUG)
	public Map<Long, List<Pigeon>> getAgentPigeons(long agentId) {
		Map<Long, List<Pigeon>> map = null;
		List<Pigeon> pigeons = pigeonDAOImp.getPigeonList(agentId);
		if (pigeons != null) {
			map = new HashMap<Long, List<Pigeon>>();
			map.put(agentId, pigeons);
		}
		return map;
	}

	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPigeon(long appId) throws PigeonServerException {
		return getPigeon(appId, null, 0);
	}

	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPigeon(long appId, String region, int redirectPort) throws PigeonServerException {
		Pigeon pigeon;
		if (region == null || region.isEmpty()) {
			pigeon = pigeonDAOImp.getPigeon(appId, redirectPort, true);
			if (pigeon == null) {
				pigeon = pigeonDAOImp.getPigeon(appId, 0, false);
				Logger.warn(this, "Retreiving pigeon with isRoundRobinFetch as false  redirectPort= %d", redirectPort);
			}
		} else {
			pigeon = pigeonDAOImp.getPigeon(appId, region, redirectPort, true);
			if (pigeon == null) {
				pigeon = pigeonDAOImp.getPigeon(appId, region, 0, false);
				Logger.warn(this, "Retreiving pigeon with isRoundRobinFetch as false region= %s redirectPort=%d",
						region, redirectPort);
			}
		}
		if (pigeon == null) {
			new GeneratePigeons(false, region, Pigeon.TYPE_L2).start();
			if (region == null || region.isEmpty()) {
				pigeon = pigeonDAOImp.getPigeon(appId, redirectPort, true);
				if (pigeon == null) {
					pigeon = pigeonDAOImp.getPigeon(appId, 0, false);
					Logger.warn(this, "Retreiving pigeon with isRoundRobinFetch as false  redirectPort= %d",
							redirectPort);
				}
			} else {
				pigeon = pigeonDAOImp.getPigeon(appId, region, redirectPort, false);
			}
		}
		if (pigeon != null) {
			brokerOperator.updateBroker(pigeon.getBrokerName(), 1, 0);
			if (pigeonDAOImp.getPigeonCount(pigeon.getBrokerName()) <= (Pigeon.PIGEONCOUNT / 3)) {
				Logger.info(this, "Pigeon Generation initiated as Pigeons are low of type : %s", Pigeon.TYPE_L2);
				new GeneratePigeons(false, region, Pigeon.TYPE_L2).start();
			}
		}
		return pigeon;
	}

	@Loggable(value = Loggable.DEBUG)
	public List<String> readClientIds(InputStream inputStream)
			throws IOException {
		String contents = IOUtils.toString(inputStream, "UTF-8");
		String[] split = contents.substring(1, contents.length() - 1).split(
				"\\s*,\\s*");
		List<String> clientIds = Arrays.asList(split);
		return clientIds;
	}

	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPigeon(String region, long appId, int redirectPort)
			throws PigeonServerException {
		return getPigeon(appId, region, redirectPort);

	}

	@Loggable(value = Loggable.DEBUG)
	public Pigeon readPigeon(String clientId) throws PigeonServerException {
		Matcher matcher = null;
		if (clientId == null || clientId.length() > Pigeon.CLIENTID_LENGTH
				|| clientId.isEmpty()) {
			throw new PigeonServerException(
					ErrorState.INVALID_CLIENTID_LEN.getMessage());
		}
		matcher = Pattern.compile(Pigeon.CLIENTID_PATTERN).matcher(clientId);
		if (!matcher.matches()) {
			throw new PigeonServerException(
					ErrorState.INVALID_CLIENTID.getMessage());
		}
		return pigeonDAOImp.getPigeonbyClientId(clientId);

	}

	/*
	 * Update pigeon and send connect-disconnect message to application server
	 * as needed.
	 * 
	 * 1. Send connect message if any one of the two pigeons gets online.
	 * 
	 * 2. Send disconnect message if both pigeons gets back to alloted from
	 * online.
	 */
	@Loggable(value = Loggable.DEBUG)
	public void updatePigeon(Pigeon pigeon) throws PigeonServerException {
		Pigeon existingPigeon = readPigeon(pigeon.getClientId());
		if (existingPigeon != null) {
			if (existingPigeon.getVersion() != pigeon.getVersion()) {
				throw new PigeonServerException("Version mismatch");
			}
			if (!existingPigeon.getStatus()
					.equalsIgnoreCase(pigeon.getStatus())) {
				existingPigeon.setStatus(pigeon.getStatus());
				pigeonDAOImp.updatePigeon(existingPigeon);
				if (pigeon.getStatus().equalsIgnoreCase(Pigeon.STATUS_ONLINE)) {
					publish(existingPigeon, ConnectionMessage.CONNECT_MESSAGE);
				} else if (pigeon.getStatus().equalsIgnoreCase(
						Pigeon.STATUS_ALLOTED)) {
					publish(existingPigeon, ConnectionMessage.ALLOCATE_MESSAGE);
				} else {
					throw new PigeonServerException(
							"Trying to update a pigeon where associated pigeon does not exists any more. Client Id: "
									+ pigeon.getClientId()
									+ ", New pigeon status: "
									+ pigeon.getStatus());
				}
			}
		} else {
			throw new PigeonServerException(
					"Trying to update a pigeon that does not exists any more. Client Id: "
							+ pigeon.getClientId() + ", New pigeon status: "
							+ pigeon.getStatus());
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deletePigeons(List<String> clientIds) {
		for (String clientId : clientIds) {
			try {
				deletePigeon(clientId);
			} catch (PigeonServerException e) {
				Logger.warn(this, "Failed to delete pigeon. ClientId: %s",
						clientId + ". Error: " + e.getMessage());
			}
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deletePigeon(String clientId) throws PigeonServerException {
		Pigeon pigeon = readPigeon(clientId);
		if (pigeon != null) {
			if (pigeon.getStatus() != Pigeon.STATUS_ONLINE) {
				deletePigeon(pigeon);
			} else {
				Logger.warn(this,
						"Cannot delete pigeon as it is online. Client Id: %s",
						clientId);
			}
		} else {
			Logger.warn(this,
					"Cannot delete pigeon as it is not present or is already deleted.");
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deletePigeon(Pigeon pigeon) throws PigeonServerException {
		if (pigeon != null && pigeonDAOImp.deletePigeon(pigeon)) {
			brokerOperator.updateBroker(pigeon.getBrokerName(), -1, -1);
			publish(pigeon, ConnectionMessage.DISCONNECT_MESSAGE);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void updatePigeonStatus(Pigeon pigeon) {
		try {
			Pigeon newPigeon = readPigeon(pigeon.getClientId());
			newPigeon.setStatus(pigeon.getStatus());
			newPigeon.setAppId(pigeon.getAppId());
			updatePigeon(newPigeon);
		} catch (PigeonServerException e) {
			Logger.warn(this, "Exception : %[exception]s", e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public List<Pigeon> getPublishPigeonList(long appId) {
		List<Pigeon> pigeonList = null;
		List<Broker> brokerList = null;
		try {
			brokerList = brokerOperator.getBrokerList(Broker.TYPE_L0, null,
					false, Broker.STATUS_ONLINE);
			if (brokerList != null) {
				for (Broker broker : brokerList) {
					if (pigeonDAOImp.getPigeonCount(broker.getBrokerName()) <= (Pigeon.PIGEONCOUNT / 3)) {
						Logger.info(
								this,
								"Pigeon Generation initiated as Pigeons are low of type : %s",
								Pigeon.TYPE_L0);
						new GeneratePigeons(true, null, Pigeon.TYPE_L0).start();
					}
				}
				pigeonList = new ArrayList<Pigeon>();
				for (Broker broker : brokerList) {
					Pigeon pigeon = pigeonDAOImp.getPublishPigeons(appId, broker.getBrokerName());
					brokerOperator.updateBroker(broker.getBrokerName(), 1, 0);
					if (pigeon != null) {
						pigeonList.add(pigeon);
					}
				}
			}
		} catch (PigeonServerException e) {
			Logger.debug(
					this,
					"Failed to get publish pigeons list. Exception : %[exception]s",
					e);
		}
		return pigeonList;
	}

	private void publish(Pigeon pigeon, char message) {
		if (pigeon.getPigeonType().equalsIgnoreCase(Pigeon.TYPE_L2)) {
			pigeonNotifier.publish(pigeon, message);
		}
	}

	private static synchronized boolean isPublishPigeonGeneration() {
		return publishPigeonGeneration;
	}

	private static synchronized boolean setPublishPigeonGeneration(
			boolean publishPigeonGeneration) {
		if (PigeonOperator.publishPigeonGeneration == publishPigeonGeneration) {
			return !publishPigeonGeneration;
		}
		PigeonOperator.publishPigeonGeneration = publishPigeonGeneration;
		return PigeonOperator.publishPigeonGeneration;
	}

	private static synchronized boolean isPigeonGeneration() {
		return pigeonGeneration;
	}

	private static synchronized boolean setPigeonGeneration(
			boolean pigeonGeneration) {
		if (PigeonOperator.pigeonGeneration == pigeonGeneration) {
			return !pigeonGeneration;
		}
		PigeonOperator.pigeonGeneration = pigeonGeneration;
		return PigeonOperator.pigeonGeneration;
	}

	private class ActivatePigeons extends Thread {
		private List<String> clientList;

		ActivatePigeons(List<String> clientList) {
			this.clientList = clientList;
		}

		public void run() {
			for (String clientId : clientList) {
				try {
					pigeonDAOImp.activatePigeons(clientId);
				} catch (Exception e) {
					Logger.warn(this,
							"Exception while activating clientId %s ", clientId);
				}
			}
		}
	}

	private class GeneratePigeons extends Thread {
		private boolean isPublishPigeon;
		private String region;
		private String pigeonType;

		GeneratePigeons(boolean isPublishPigeon, String region,
				String pigeonType) {
			this.isPublishPigeon = isPublishPigeon;
			this.region = region;
			this.pigeonType = pigeonType;
		}

		public void run() {
			// distribute pigeons in create state
			if (isPublishPigeon) {
				if (!isPublishPigeonGeneration()
						&& setPublishPigeonGeneration(true)) {
					pigeonDistributor.distributePigeons(pigeonType);
					pigeonGenerator.generatePublishPigeons(
							Pigeon.PIGEONCOUNT, region);
					sleep();
					pigeonDistributor.distributePigeons(pigeonType);
					sleep();
					setPublishPigeonGeneration(false);
				}
			} else {
				if (!isPigeonGeneration() && setPigeonGeneration(true)) {
					pigeonDistributor.distributePigeons(pigeonType);
					pigeonGenerator.generatePigeons(Pigeon.PIGEONCOUNT,
							region);
					sleep();
					pigeonDistributor.distributePigeons(pigeonType);
					sleep();
					setPigeonGeneration(false);
				}
			}
		}

		private void sleep() {
			try {
				Thread.sleep(Pigeon.SLEEP);
			} catch (InterruptedException e) {
				Logger.warn(this,
						"InterruptedException occured in sleep in PigeonOperator:sleep()");
			}
		}

	}

	public Pigeon replacePigeon(long appId, String clientId) {
		if (clientId != null && !clientId.isEmpty()) {
			clientId = clientId.replaceAll(":", "/");
		}
		return pigeonDAOImp.getReplacePigeon(appId, clientId);
	}
}
