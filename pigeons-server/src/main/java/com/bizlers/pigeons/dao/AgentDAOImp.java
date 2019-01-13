package com.bizlers.pigeons.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.bizlers.pigeons.models.Agent;
import com.bizlers.pigeons.models.Agent_;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.jcabi.aspects.Loggable;

/**
 * @author saurabh
 * 
 */
@Repository
public class AgentDAOImp implements AgentDAO {

	@Qualifier("pigeonsServerEmf")
	@PersistenceContext(unitName="com.bizlers.pigeons")
	private EntityManager entityManager;
	
	@Override
	@Loggable(value = Loggable.DEBUG)
	@Transactional("pigeonServerTxManager")
	public void createAgent(Agent agent) {
		try {
			entityManager.persist(agent);
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	@Transactional("pigeonServerTxManager")
	public void updateAgent(Agent agent) {
		try {
			entityManager.merge(agent);
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public List<Agent> getAgent(String ip) {
		List<Agent> agentList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Agent> criteriaQuery = criteriaBuilder
					.createQuery(Agent.class);
			Root<Agent> rootagent = criteriaQuery.from(Agent.class);
			criteriaQuery.select(rootagent);
			criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.equal(
					rootagent.get(Agent_.ip), ip), criteriaBuilder.notEqual(
					rootagent.get(Agent_.status), Agent.STATUS_DELETED)));
			agentList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (agentList == null || agentList.isEmpty()) {
				return null;
			} else {
				return agentList;
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Agent getAgent(String agentName, String ip)
			throws PigeonServerException {
		List<Agent> agentList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Agent> criteriaQuery = criteriaBuilder
					.createQuery(Agent.class);
			Root<Agent> rootagent = criteriaQuery.from(Agent.class);
			criteriaQuery.select(rootagent);
			Predicate p1 = criteriaBuilder.equal(
					rootagent.get(Agent_.agentName), agentName);
			Predicate p2 = criteriaBuilder.equal(rootagent.get(Agent_.ip), ip);
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			agentList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (agentList == null || agentList.isEmpty()) {
				return null;
			} else if (agentList.size() > 1) {
				throw new PigeonServerException("DUPLICATE RECORDS");
			} else if (agentList.get(0).getStatus()
					.equalsIgnoreCase(Agent.STATUS_DELETED)) {
				throw new PigeonServerException("AGENT DELETED");
			} else {
				return agentList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public void removeAgent(Agent agent) {
		updateAgent(agent);
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Agent getAgent(long agentId) {
		Agent agent = null;
		try {
			agent = entityManager.find(Agent.class, agentId);
		} finally {
			entityManager.close();
		}
		return agent;
	}

}