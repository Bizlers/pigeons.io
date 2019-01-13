package com.bizlers.pigeons.utils;

import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bizlers.auth.commons.core.SessionPrincipal;
import com.bizlers.auth.commons.dao.UserRoleDAO;
import com.bizlers.auth.commons.models.Session;
import com.bizlers.auth.commons.models.UserRole;
import com.bizlers.pigeons.core.AgentOperator;
import com.bizlers.pigeons.core.BrokerOperator;
import com.bizlers.pigeons.core.PigeonOperator;
import com.bizlers.pigeons.dao.ApplicationDAO;
import com.bizlers.pigeons.models.Agent;
import com.bizlers.pigeons.models.Application;
import com.bizlers.pigeons.models.Broker;
import com.bizlers.pigeons.models.Pigeon;
import com.jcabi.log.Logger;

@Component
public class Validator {
	
	public static final String AGENT_ROLE = "agent";
	
	private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	
	@Autowired
	private UserRoleDAO userRoleDao;
	
	@Autowired
	private PigeonOperator pigeonOperator;
	
	@Autowired
	private BrokerOperator brokerOperator;
	
	@Autowired
	private AgentOperator agentOperator;
	
	@Autowired
	private ApplicationDAO applicationDAO;

	public boolean isValidRequest(Agent agent, Agent oldAgent,
			Session session) {
		if (agent.getAccountId() == oldAgent.getAccountId()
				&& oldAgent.getAccountId() == session.getAccountId()) {
			return true;
		}
		return false;
	}

	public boolean isValidApplication(Application application,
			Session session) {
		if (session.getAccountId() == application.getAccountId()) {
			application.setAppId(0);
			application.setState(0);
			return true;
		}
		return false;
	}

	public boolean isValidApplication(Application application,
			long applicationId, Session session) {
		try {
			Application oldApplication = applicationDAO.get(applicationId);
			if (application.getEmailId() != null) {
				if (session.getAccountId() == application.getAccountId()
						&& application.getEmailId().matches(EMAIL_PATTERN)
						&& application.getAccountId() == oldApplication
								.getAccountId()) {
					if (!application.getEmailId().equalsIgnoreCase(
							oldApplication.getEmailId())
							&& applicationDAO.get(application.getEmailId()) != null) {
						return false;
					}
					application.setState(oldApplication.getState());
					return true;
				}
			}
		} catch (PigeonServerException e) {
			Logger.warn(
					Validator.class,
					"Exception while validating applcation. Exception : %[exception]s",
					e);
		}
		return false;
	}

	public boolean isValidRequest(Broker broker,
			SecurityContext securityContext) {
		SessionPrincipal sessionPrincipal = (SessionPrincipal) securityContext.getUserPrincipal();
		Agent agent = agentOperator.readAgent(broker.getAgentId());
		if (agent != null && agent.getAccountId() == sessionPrincipal.getSession().getAccountId()) {
			return true;
		}
		return false;
	}

	public boolean isValidRequest(Broker broker, Session session) {
		Agent agent = agentOperator.readAgent(broker.getAgentId());
		if (agent != null && agent.getAccountId() == session.getAccountId()) {
			return true;
		}
		return false;
	}

	public boolean isValidRequest(String brokerName,
			SecurityContext securityContext) {
		Session session = ((SessionPrincipal) securityContext.getUserPrincipal()).getSession();
		Agent agent;
		try {
			agent = agentOperator.readAgent(brokerOperator
					.readBroker(brokerName).getAgentId());
			if (agent != null && agent.getAccountId() == session.getAccountId()) {
				return true;
			}
		} catch (PigeonServerException e) {
			Logger.warn(Validator.class, "Exception : %[exception]s", e);
		}
		return false;
	}

	public boolean isValidAppRequest(long appId, SecurityContext securityContext) {
		Session session = ((SessionPrincipal) securityContext.getUserPrincipal()).getSession();
		Application application;
		try {
			application = applicationDAO.get(appId);
			if (application != null && application.getAccountId() == session.getAccountId()) {
				if (userRoleDao.isUserInRole(session.getAccountId(), UserRole.ROLE_APPLICATION)) {
					return true;
				} else {
					Logger.warn(Validator.class, "User not in application role");
				}
			}
			Logger.warn(Validator.class, "The request for appId %d is not valid.", appId);
		} catch (PigeonServerException e) {
			Logger.warn(Validator.class, "Exception while validating app request. Exception : %[exception]s", e);
		}
		return false;
	}

	public boolean isValidAppRequest(Pigeon pigeon, Session session) {
		Application application;
		try {
			application = applicationDAO.get(pigeonOperator
					.readPigeon(pigeon.getClientId()).getAppId());
			if (application != null
					&& (application.getAccountId() == session.getAccountId() || 
					userRoleDao.isUserInRole(session.getAccountId(), AGENT_ROLE))) {
				return true;
			}
		} catch (PigeonServerException e) {
			Logger.warn(
					Validator.class,
					"Exception while validating app request. Exception : %[exception]s",
					e);
		}
		return false;
	}
	
	public boolean isUserInRole(SecurityContext context, String role) {
		Session session = (Session) ((SessionPrincipal) context.getUserPrincipal()).getSession();
		long userId = session.getAccountId();
		return userRoleDao.isUserInRole(userId, role);
	}
}