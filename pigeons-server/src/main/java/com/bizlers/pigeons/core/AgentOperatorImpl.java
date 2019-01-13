package com.bizlers.pigeons.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bizlers.auth.commons.dao.UserRoleDAO;
import com.bizlers.auth.commons.utils.AuthorizationException;
import com.bizlers.pigeons.dao.AgentDAO;
import com.bizlers.pigeons.dao.BrokerDAO;
import com.bizlers.pigeons.dao.PigeonDAO;
import com.bizlers.pigeons.models.Agent;
import com.bizlers.pigeons.models.Broker;
import com.bizlers.pigeons.utils.ErrorState;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.jcabi.aspects.Loggable;

/**
 * @author saurabh
 * 
 */
@Service
public class AgentOperatorImpl implements AgentOperator {
	
	private static final String AGENT_ROLE = "agent";

	private boolean update;
	
	@Autowired
	private UserRoleDAO userRoleDao;
	
	@Autowired
	private AgentDAO agentDAOImp; 
	
	@Autowired
	private BrokerDAO brokerDAOImp;
	
	@Autowired
	private PigeonDAO pigeonDAOImp;
	
	@Loggable(value = Loggable.DEBUG)
	private boolean isValidateAgent(Agent agent) throws PigeonServerException {
		if (agent.getAgentName() == null || agent.getAgentName().isEmpty()
				|| agent.getAgentName().length() > Agent.AGENTNAME_LENGTH) {
			throw new com.bizlers.pigeons.utils.PigeonServerException(
					ErrorState.INVALID_AGENTNAME_LEN.getMessage());
		} else if (agent.getHostName() == null
				|| agent.getHostName().length() > Agent.HOSTNAME_LENGTH
				|| agent.getHostName().isEmpty()) {
			throw new PigeonServerException(
					ErrorState.INVALID_MCNAME.getMessage());
		} else if (agent.getIp() == null || agent.getIp().isEmpty()
				|| agent.getIp().length() > Agent.IP_LENGTH) {
			throw new PigeonServerException(ErrorState.INVALID_IP.getMessage());
		} else if (agentDAOImp.getAgent(agent.getAgentName(), agent.getIp()) != null
				&& !update) {
			throw new PigeonServerException(
					ErrorState.DUPLICATE_AGENT.getMessage());
		} else {
			return true;
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void createAgent(Agent agent) throws PigeonServerException {
		update = false;
		if (isValidateAgent(agent)) {
			try {
				userRoleDao.insertUserRole(agent.getAccountId(), AGENT_ROLE);
			} catch (Exception e) {
				throw new PigeonServerException(e);
			}
			agent.setStatus(Agent.STATUS_ONLINE);
			agent.setCreated(new java.util.Date());
			agent.setLastUpdated(new java.util.Date());
			agent.setVersion(0);
			agentDAOImp.createAgent(agent);
		} else {
			throw new PigeonServerException(ErrorState.FAILURE.getMessage());
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public List<Agent> getAgentByIp(String ip) throws PigeonServerException {
		return agentDAOImp.getAgent(ip);
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Agent readAgent(String agentName, String ip)
			throws PigeonServerException {
		if (agentName == null || agentName.isEmpty()
				|| agentName.length() > Agent.AGENTNAME_LENGTH) {
			throw new PigeonServerException(
					ErrorState.INVALID_AGENTNAME_LEN.getMessage());
		}
		if (ip == null || ip.isEmpty() || ip.length() > Agent.IP_LENGTH) {
			throw new PigeonServerException(ErrorState.INVALID_IP.getMessage());
		}

		return agentDAOImp.getAgent(agentName, ip);
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Agent readAgent(long agentId) {
		return agentDAOImp.getAgent(agentId);
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	@Transactional("pigeonServerTxManager")
	public void updateAgent(Agent agent) throws PigeonServerException {
		Agent agentOld = new Agent();
		update = true;
		agentOld = readAgent(agent.getAgentName(), agent.getIp());
		if (agentOld != null && isValidateAgent(agent)) {
			if (agentOld.getVersion() != agent.getVersion()) {
				throw new PigeonServerException("Version mismatch");
			}
			if (agentOld.getStatus().equalsIgnoreCase(Agent.STATUS_ONLINE)
					&& (agent.getStatus()
							.equalsIgnoreCase(Agent.STATUS_OFFLINE) || agent
							.getStatus().equalsIgnoreCase(Agent.STATUS_DELETED))) {
				brokerCleanup(agent);
			}
			agent.setLastUpdated(new java.util.Date());
			agent.setAgentId(agentOld.getAgentId());
			agent.setCreated(agentOld.getCreated());
			agent.setAgentName(agentOld.getAgentName());
			agentDAOImp.updateAgent(agent);
		} else {
			throw new PigeonServerException(ErrorState.FAILURE.getMessage());
		}
	}

	private void brokerCleanup(Agent agent) throws PigeonServerException {
		List<Broker> brokers = brokerDAOImp.getBrokers(agent.getAgentId());
		if (brokers != null) {
			for (Broker broker : brokers) {
				broker.setStatus(Broker.STATUS_OFFLINE);
				broker.setAlloted(0);
				broker.setPigeonsCreated(0);
				brokerDAOImp.updateBroker(broker);
				pigeonDAOImp.deleteAllPigeons(broker.getBrokerName());
			}
			pigeonDAOImp.deleteAllPigeonsOfAgent(agent.getAgentId());
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void removeAgent(String agentname, String ip) throws PigeonServerException {
		Agent agent = readAgent(agentname, ip);
		if (agent != null) {
			try {
				userRoleDao.deleteUserRole(agent.getAccountId(), AGENT_ROLE);
			} catch (AuthorizationException e) {
				throw new PigeonServerException(e);
			}
			agent.setStatus(Agent.STATUS_DELETED);
			agentDAOImp.removeAgent(agent);
		} else {
			throw new PigeonServerException(ErrorState.FAILURE.getMessage());
		}
	}
}
