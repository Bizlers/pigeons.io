package com.bizlers.pigeons.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bizlers.pigeons.dao.AgentDAO;
import com.bizlers.pigeons.dao.BrokerDAO;
import com.bizlers.pigeons.dao.PigeonDAO;
import com.bizlers.pigeons.models.Broker;
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
@Scope("singleton")
public class BrokerOperatorImpl implements BrokerOperator {

	private boolean updateFlag = false;
	
	@Autowired
	private BrokerDAO brokerDAOImp;
	
	@Autowired
	private AgentDAO agentDAOImp;
	
	@Autowired
	private PigeonDAO pigeonDAO;

	@Loggable(value = Loggable.DEBUG)
	public int getBrokerCount(String type) throws PigeonServerException {
		return brokerDAOImp.getBrokerCount(type);
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Broker getBroker(String type, String status)
			throws PigeonServerException {
		if (status != null
				&& !(status.equalsIgnoreCase(Broker.STATUS_OFFLINE)
						|| status.equalsIgnoreCase(Broker.STATUS_ONLINE) || status
							.equalsIgnoreCase(Broker.STATUS_ALL))) {
			throw new PigeonServerException("Invalid Broker Status");
		} else {
			return brokerDAOImp.getBroker(type, status);
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Broker getBroker(String type, String region, String status)
			throws PigeonServerException {
		if (status != null
				&& !(status.equalsIgnoreCase(Broker.STATUS_OFFLINE)
						|| status.equalsIgnoreCase(Broker.STATUS_ONLINE) || status
							.equalsIgnoreCase(Broker.STATUS_ALL))) {
			throw new PigeonServerException("Invalid Broker Status");
		} else {
			Broker broker = brokerDAOImp.getBroker(type, region, status);
			if (broker == null) {
				broker = brokerDAOImp.getBroker(type, status);
			}
			return broker;
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public List<Broker> getBrokerList(String type, String region,
			boolean isMaxLimit, String status) throws PigeonServerException {
		if (status != null
				&& !(status.equalsIgnoreCase(Broker.STATUS_OFFLINE)
						|| status.equalsIgnoreCase(Broker.STATUS_ONLINE) || status
							.equalsIgnoreCase(Broker.STATUS_ALL))) {
			throw new PigeonServerException("Invalid Broker Status");
		} else {
			List<Broker> brokerList;
			if (region == null || region.isEmpty()) {
				brokerList = brokerDAOImp.getBrokers(type, isMaxLimit, status);
			} else {
				brokerList = brokerDAOImp.getBrokers(type, region, isMaxLimit,
						status);
			}
			return brokerList;
		}
	}

	@Loggable(value = Loggable.DEBUG)
	private boolean isValidateBroker(Broker broker)
			throws PigeonServerException {
		if (broker.getAgentId() == 0) {
			throw new PigeonServerException(
					ErrorState.INVALID_AGENTID.getMessage());
		} else if (broker.getBrokerType() == null
				|| broker.getBrokerType().isEmpty()) {
			throw new PigeonServerException(
					ErrorState.INVALID_BROKERTYPE.getMessage());
		} else if (broker.getRegion() == null || broker.getRegion().isEmpty()) {
			throw new PigeonServerException(
					ErrorState.INVALID_REGION.getMessage());
		} else if (broker.getPort() < 0 || broker.getPort() == 0) {
			throw new PigeonServerException(
					ErrorState.INVALID_PORT.getMessage());
		} else if (broker.getMaxLimit() <= 0) {
			throw new PigeonServerException(
					ErrorState.INVALID_MAXLIMIT.getMessage());
		} else if (broker.getIp() == null || broker.getIp().isEmpty()
				|| broker.getIp().length() > Broker.IP_LENGTH) {
			throw new PigeonServerException(ErrorState.INVALID_IP.getMessage());
		} else if (brokerDAOImp.getBroker(broker.getIp(), broker.getPort()) != null
				&& !updateFlag) {
			throw new PigeonServerException(
					ErrorState.DUPLICATE_BROKER.getMessage());
		} else if (agentDAOImp.getAgent(broker.getAgentId()) == null
				&& !updateFlag) {
			throw new PigeonServerException(
					ErrorState.INVALID_AGENT.getMessage());
		} else if (!isValidateConnections(broker)) {
			throw new PigeonServerException(
					ErrorState.INVALID_PARENT_BROKER.getMessage());
		} else {
			return true;
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public boolean isValidateConnections(Broker broker) {
		List<Broker> brokerConnections = broker.getConnectedTo();
		boolean flag = false;
		int i = 0;
		if (brokerConnections != null) {
			for (i = 0; i < brokerConnections.size(); i++) {
				try {
					if (brokerDAOImp.getBrokerByName(brokerConnections.get(i)
							.getBrokerName()) == null) {
						break;
					}
				} catch (PigeonServerException e) {
					Logger.warn(this, "Exception : %[exception]s", e);
				}
			}
			if (brokerConnections.size() == i) {
				flag = true;
			}
		} else {
			flag = true;
		}
		return flag;
	}

	@Loggable(value = Loggable.DEBUG)
	public String getBrokerName() throws PigeonServerException {
		String name = null;
		int i = 0;
		while (i < Broker.RETRYCOUNT) {
			name = Generator.getUniqueName();
			if (brokerDAOImp.getBrokerByName(name) == null) {
				break;
			} else {
				i++;
			}
			if (i == Broker.RETRYCOUNT) {
				throw new PigeonServerException(
						ErrorState.MAX_RETRY.getMessage());
			}
		}
		return name;
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void createBroker(Broker broker) throws PigeonServerException {
		updateFlag = false;
		if (isValidateBroker(broker)) {
			broker.setAlloted(0);
			broker.setCreated(new java.util.Date());
			broker.setLastUpdated(new java.util.Date());
			broker.setStatus(Broker.STATUS_ONLINE);
			broker.setPigeonsCreated(0);
			broker.setBrokerName(getBrokerName());
			brokerDAOImp.createBroker(broker);
			if (broker.getBrokers() != null) {
				updateBrokerConnections(broker);
			}
		} else {
			throw new PigeonServerException(ErrorState.FAILURE.getMessage());
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Broker readBroker(String brokername) throws PigeonServerException {
		Matcher matcher = null;
		if (brokername == null || brokername.isEmpty()
				|| brokername.length() > Broker.BROKERNAME_LENGTH) {
			throw new PigeonServerException(
					ErrorState.INVALID_BROKERNAME_LEN.getMessage());
		}
		matcher = Pattern.compile(Broker.BROKERNAME_PATTERN)
				.matcher(brokername);
		if (!matcher.matches()) {
			throw new PigeonServerException(
					ErrorState.INVALID_BROKERNAME.getMessage());
		}
		return brokerDAOImp.getBrokerByName(brokername);
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Broker readBroker(String ip, int port) throws PigeonServerException {
		return brokerDAOImp.getBroker(ip, port);
	}

	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void updateBrokerConnections(Broker broker) throws PigeonServerException {
		if (broker.getBrokers() != null && broker.getBrokers().length() > 0) {
			List<Broker> connectedTo = broker.getConnectedTo();
			if (connectedTo == null) {
				connectedTo = new ArrayList<>();
				broker.setConnectedTo(connectedTo);
			}
			String brokers[] = broker.getBrokers().split(":");
			for (String brokerName : brokers) {
				try {
					Broker connected = brokerDAOImp.getBrokerByName(brokerName);
					if (connected != null) {
						connectedTo.add(connected);
						broker.setBridgeCount(broker.getBridgeCount() + 1);
					}
					connected.getConnectedTo().add(broker);
					connected.setBridgeCount(connected.getBridgeCount() + 1);
					brokerDAOImp.updateBroker(connected);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Loggable(value = Loggable.DEBUG)
	private void removeBrokerConnections(Broker broker) throws PigeonServerException {
		for (Broker brokerInfo : broker.getConnectedTo()) {
			Broker connected = brokerDAOImp.getBrokerByName(brokerInfo.getBrokerName());
			if (connected.getStatus().equalsIgnoreCase(Broker.STATUS_ONLINE)) {
				connected.setBridgeCount(brokerInfo.getBridgeCount() - 1);
				removeBroker(connected, broker);
				broker.setBridgeCount(broker.getBridgeCount() - 1);
				brokerDAOImp.updateBroker(connected);
			}
		}
		broker.setConnectedTo(null);
	}

	private void removeBroker(Broker broker, Broker connectedBroker) {
		if (broker.getConnectedTo() != null) {
			for (int i = 0; i < broker.getConnectedTo().size(); i++) {
				Broker broker1 = broker.getConnectedTo().get(i);
				if (broker1.getId() == connectedBroker.getId()) {
					broker.getConnectedTo().remove(i);
				}
			}
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public synchronized void updateBroker(Broker broker) throws PigeonServerException {
		System.out.println("Updating broker: " + broker.getBrokerName());
		Broker oldBroker = new Broker();
		oldBroker = readBroker(broker.getBrokerName());
		updateFlag = true;
		if (oldBroker != null && isValidateBroker(broker)) {
			if (broker.getVersion() != oldBroker.getVersion()) {
				throw new PigeonServerException("Version mismatch. broker version: " + broker.getVersion()
						+ ", expected version: " + oldBroker.getVersion());
			}
			broker.setId(oldBroker.getId());
			broker.setBridgeCount(oldBroker.getBridgeCount());
			if (oldBroker.getStatus().equalsIgnoreCase(Broker.STATUS_ONLINE)
					&& (broker.getStatus().equalsIgnoreCase(Broker.STATUS_OFFLINE)
							|| broker.getStatus().equalsIgnoreCase(Broker.STATUS_DELETED))) {
				broker.setCreated(oldBroker.getCreated());
				broker.setLastUpdated(new java.util.Date());
				broker.setAlloted(0);
				broker.setPigeonsCreated(0);
				broker.setRedirectPort(0);
				brokerDAOImp.updateBroker(broker);
				if (broker.getConnectedTo() != null) {
					removeBrokerConnections(broker);
				}
				pigeonDAO.deleteAllPigeons(broker.getBrokerName());
			}
			if (oldBroker.getStatus().equalsIgnoreCase(Broker.STATUS_OFFLINE)
					&& broker.getStatus().equalsIgnoreCase(Broker.STATUS_ONLINE)) {
				if (broker.getRedirectPort() != 0
						&& brokerDAOImp.getRedirectBroker(broker.getIp(), broker.getRedirectPort()) != null) {
					throw new PigeonServerException(
							"Broker with existing redirect port already running, Try using another redirect port.");
				}
				broker.setAlloted(0);
				broker.setPigeonsCreated(0);
				broker.setCreated(oldBroker.getCreated());
				broker.setLastUpdated(new java.util.Date());
				updateBrokerConnections(broker);
				brokerDAOImp.updateBroker(broker);
			}
		} else {
			throw new PigeonServerException(ErrorState.FAILURE.getMessage());
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public void updateBroker(String brokerName, int alloted, int pigeonCreated) throws PigeonServerException {
		Broker broker = brokerDAOImp.getBrokerByName(brokerName);
		System.out.println("Updating broker: " + broker.getBrokerName());
		broker.setAlloted(broker.getAlloted() + alloted);
		broker.setPigeonsCreated(broker.getPigeonsCreated() + pigeonCreated);
		brokerDAOImp.updateBroker(broker);
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public void updateBroker(long brokerId, int alloted, int pigeonCreated) throws PigeonServerException {
		Broker broker = brokerDAOImp.getBroker(brokerId);
		System.out.println("Updating broker: " + broker.getBrokerName());
		broker.setAlloted(broker.getAlloted() + alloted);
		broker.setPigeonsCreated(broker.getPigeonsCreated() + pigeonCreated);
		brokerDAOImp.updateBroker(broker);
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public void removeBroker(String brokername) throws PigeonServerException {
		Broker broker = readBroker(brokername);
		if (broker != null) {
			System.out.println("Removing broker: " + broker.getBrokerName());
			broker.setStatus(Broker.STATUS_DELETED);
			brokerDAOImp.removeBroker(broker);
		} else {
			throw new PigeonServerException(ErrorState.FAILURE.getMessage());
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public List<Broker> getAgentBrokers(long agentId) {
		return brokerDAOImp.getAgentBrokersForPigeonGeneration(agentId);
	}
}