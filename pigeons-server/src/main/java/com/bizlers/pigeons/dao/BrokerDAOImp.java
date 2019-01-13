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

import com.bizlers.pigeons.models.Broker;
import com.bizlers.pigeons.models.Broker_;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.jcabi.aspects.Loggable;

/**
 * @author saurabh
 * 
 */
@Repository
public class BrokerDAOImp implements BrokerDAO {

	@Qualifier("pigeonsServerEmf")
	@PersistenceContext(unitName="com.bizlers.pigeons")
	private EntityManager entityManager;

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void createBroker(Broker broker) {
		try {
			entityManager.persist(broker);
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable
	public Broker getBroker(long brokerId) {
		try {
			return entityManager.find(Broker.class, brokerId);
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Broker getBrokerByName(String brokerName)
			throws PigeonServerException {
		List<Broker> brokerList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Broker> criteriaQuery = criteriaBuilder
					.createQuery(Broker.class);
			Root<Broker> rootbroker = criteriaQuery.from(Broker.class);
			criteriaQuery.select(rootbroker);
			criteriaQuery.where(criteriaBuilder.equal(
					rootbroker.get(Broker_.brokerName), brokerName));
			brokerList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (brokerList == null || brokerList.isEmpty()) {
				return null;
			} else if (brokerList.size() > 1) {
				throw new PigeonServerException("DUPLICATE RECORDS");
			} else if (brokerList.get(0).getStatus()
					.equalsIgnoreCase(Broker.STATUS_DELETED)) {
				throw new PigeonServerException("BROKER DELETED");
			} else {
				return brokerList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public Broker updateBroker(Broker broker) {
		try {
			return entityManager.merge(broker);
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public void removeBroker(Broker broker) {
		updateBroker(broker);
	}

	@Loggable(value = Loggable.DEBUG)
	public Broker getRedirectBroker(String ip, int redirectPort)
			throws PigeonServerException {
		List<Broker> brokerList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Broker> criteriaQuery = criteriaBuilder
					.createQuery(Broker.class);
			Root<Broker> rootBroker = criteriaQuery.from(Broker.class);
			criteriaQuery.select(rootBroker);
			Predicate p1 = (criteriaBuilder.equal(rootBroker.get(Broker_.ip),
					ip));
			Predicate p2 = (criteriaBuilder.equal(
					rootBroker.get(Broker_.redirectPort), redirectPort));
			Predicate p3 = (criteriaBuilder.notEqual(
					rootBroker.get(Broker_.status), Broker.STATUS_DELETED));
			Predicate p4 = criteriaBuilder.and(p1, p2);
			criteriaQuery.where(criteriaBuilder.and(p3, p4));
			brokerList = entityManager.createQuery(criteriaQuery)
					.setMaxResults(1).getResultList();
			if (brokerList == null || brokerList.isEmpty()) {
				return null;
			} else if (brokerList.size() > 1) {
				throw new PigeonServerException("DUPLICATE RECORDS");
			} else {
				return brokerList.get(0);
			}
		} finally {
			entityManager.close();
		}

	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Broker getBroker(String ip, int port) throws PigeonServerException {
		List<Broker> brokerList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Broker> criteriaQuery = criteriaBuilder
					.createQuery(Broker.class);
			Root<Broker> rootBroker = criteriaQuery.from(Broker.class);
			criteriaQuery.select(rootBroker);
			Predicate p1 = (criteriaBuilder.equal(rootBroker.get(Broker_.ip),
					ip));
			Predicate p2 = (criteriaBuilder.equal(rootBroker.get(Broker_.port),
					port));
			Predicate p3 = (criteriaBuilder.notEqual(
					rootBroker.get(Broker_.status), Broker.STATUS_DELETED));
			Predicate p4 = criteriaBuilder.and(p1, p2);
			criteriaQuery.where(criteriaBuilder.and(p3, p4));
			brokerList = entityManager.createQuery(criteriaQuery)
					.setMaxResults(1).getResultList();
			if (brokerList == null || brokerList.isEmpty()) {
				return null;
			} else if (brokerList.size() > 1) {
				throw new PigeonServerException("DUPLICATE RECORDS");
			} else {
				return brokerList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public int getBrokerCount(String type) {
		List<Broker> brokerList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Broker> criteriaQuery = criteriaBuilder
					.createQuery(Broker.class);
			Root<Broker> rootBroker = criteriaQuery.from(Broker.class);
			criteriaQuery.select(rootBroker);
			Predicate p1 = (criteriaBuilder.equal(
					rootBroker.get(Broker_.brokerType), type));
			Predicate p2 = (criteriaBuilder.notEqual(
					rootBroker.get(Broker_.status), Broker.STATUS_DELETED));
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			brokerList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (brokerList == null || brokerList.isEmpty()) {
				return 0;
			} else {
				return brokerList.size();
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Broker getBroker(String type, String status) {
		List<Broker> brokerList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Broker> criteriaQuery = criteriaBuilder
					.createQuery(Broker.class);
			Root<Broker> rootBroker = criteriaQuery.from(Broker.class);
			criteriaQuery.select(rootBroker);
			Predicate p1 = (criteriaBuilder.equal(
					rootBroker.get(Broker_.brokerType), type));
			Predicate p2 = null;
			if (status.equalsIgnoreCase(Broker.STATUS_ALL)) {
				p2 = (criteriaBuilder.notEqual(rootBroker.get(Broker_.status),
						Broker.STATUS_DELETED));
			} else {
				p2 = criteriaBuilder.and((criteriaBuilder.equal(
						rootBroker.get(Broker_.status), status)),
						(criteriaBuilder.notEqual(
								rootBroker.get(Broker_.status),
								Broker.STATUS_DELETED)));
			}
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			criteriaQuery.orderBy(criteriaBuilder.asc(rootBroker
					.get(Broker_.bridgeCount)));
			brokerList = entityManager.createQuery(criteriaQuery)
					.setMaxResults(1).getResultList();
			if (brokerList == null || brokerList.isEmpty()) {
				return null;
			} else {
				return brokerList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Broker getBroker(String type, String region, String status) {
		List<Broker> brokerList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Broker> criteriaQuery = criteriaBuilder
					.createQuery(Broker.class);
			Root<Broker> rootBroker = criteriaQuery.from(Broker.class);
			criteriaQuery.select(rootBroker);
			Predicate p1 = (criteriaBuilder.equal(
					rootBroker.get(Broker_.brokerType), type));
			Predicate p2 = (criteriaBuilder.equal(
					rootBroker.get(Broker_.region), region));
			Predicate p3 = null;
			if (status.equalsIgnoreCase(Broker.STATUS_ALL)) {
				p3 = (criteriaBuilder.notEqual(rootBroker.get(Broker_.status),
						Broker.STATUS_DELETED));
			} else {
				p3 = criteriaBuilder.and((criteriaBuilder.equal(
						rootBroker.get(Broker_.status), status)),
						(criteriaBuilder.notEqual(
								rootBroker.get(Broker_.status),
								Broker.STATUS_DELETED)));
			}
			criteriaQuery.where(criteriaBuilder.and(
					criteriaBuilder.and(p2, p1), p3));
			criteriaQuery.orderBy(criteriaBuilder.asc(rootBroker
					.get(Broker_.bridgeCount)));
			brokerList = entityManager.createQuery(criteriaQuery)
					.setMaxResults(1).getResultList();
			if (brokerList == null || brokerList.isEmpty()) {
				return null;
			} else {
				return brokerList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public List<Broker> getAgentBrokersForPigeonGeneration(long agentId) {
		List<Broker> brokerList = null;
		try {

			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Broker> criteriaQuery = criteriaBuilder
					.createQuery(Broker.class);
			Root<Broker> rootBroker = criteriaQuery.from(Broker.class);
			criteriaQuery.select(rootBroker);
			Predicate p1 = (criteriaBuilder.notEqual(
					rootBroker.get(Broker_.brokerType), Broker.TYPE_L1));
			Predicate p2 = criteriaBuilder.equal(
					rootBroker.get(Broker_.agentId), agentId);
			Predicate p3 = criteriaBuilder.equal(
					rootBroker.get(Broker_.status), Broker.STATUS_ONLINE);
			Predicate p4 = criteriaBuilder.and(p1, p2);
			criteriaQuery.where(criteriaBuilder.and(p3, p4));
			brokerList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (brokerList == null || brokerList.isEmpty()) {
				return null;
			}
			return brokerList;
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public List<Broker> getBrokers(String type, String region,
			boolean isMaxLimit, String status) {
		List<Broker> brokerList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Broker> criteriaQuery = criteriaBuilder
					.createQuery(Broker.class);
			Root<Broker> rootBroker = criteriaQuery.from(Broker.class);
			criteriaQuery.select(rootBroker);
			Predicate p1 = (criteriaBuilder.equal(
					rootBroker.get(Broker_.region), region));
			Predicate p2 = null;
			if (isMaxLimit) {
				p2 = (criteriaBuilder.greaterThan(
						rootBroker.get(Broker_.maxLimit),
						rootBroker.get(Broker_.pigeonsCreated)));
			} else {
				p2 = criteriaBuilder.equal(rootBroker.get(Broker_.maxLimit),
						rootBroker.get(Broker_.maxLimit));
			}
			Predicate p3 = (criteriaBuilder.equal(
					rootBroker.get(Broker_.brokerType), type));
			Predicate p4 = criteriaBuilder.and(p1, p2);
			Predicate p5 = criteriaBuilder.and(p4, p3);
			Predicate p6 = null;
			if (status.equalsIgnoreCase(Broker.STATUS_ALL)) {
				p6 = (criteriaBuilder.notEqual(rootBroker.get(Broker_.status),
						Broker.STATUS_DELETED));
			} else {
				p6 = criteriaBuilder.and((criteriaBuilder.equal(
						rootBroker.get(Broker_.status), status)),
						(criteriaBuilder.notEqual(
								rootBroker.get(Broker_.status),
								Broker.STATUS_DELETED)));
			}
			criteriaQuery.where(criteriaBuilder.and(p6, p5));
			criteriaQuery.orderBy(criteriaBuilder.asc(rootBroker
					.get(Broker_.pigeonsCreated)));
			brokerList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (brokerList == null || brokerList.isEmpty()) {
				return null;
			} else {
				return brokerList;
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public List<Broker> getBrokers(String type, boolean isMaxLimit,
			String status) {
		List<Broker> brokerList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Broker> criteriaQuery = criteriaBuilder
					.createQuery(Broker.class);
			Root<Broker> rootBroker = criteriaQuery.from(Broker.class);
			criteriaQuery.select(rootBroker);
			Predicate p1 = null;
			if (status.equalsIgnoreCase(Broker.STATUS_ALL)) {
				p1 = (criteriaBuilder.notEqual(rootBroker.get(Broker_.status),
						Broker.STATUS_DELETED));
			} else {
				p1 = criteriaBuilder.and((criteriaBuilder.equal(
						rootBroker.get(Broker_.status), status)),
						(criteriaBuilder.notEqual(
								rootBroker.get(Broker_.status),
								Broker.STATUS_DELETED)));
			}
			Predicate p2 = null;
			if (isMaxLimit) {
				p2 = (criteriaBuilder.greaterThan(
						rootBroker.get(Broker_.maxLimit),
						rootBroker.get(Broker_.pigeonsCreated)));
			} else {
				p2 = (criteriaBuilder.equal(rootBroker.get(Broker_.maxLimit),
						rootBroker.get(Broker_.maxLimit)));
			}
			Predicate p3 = (criteriaBuilder.equal(
					rootBroker.get(Broker_.brokerType), type));
			Predicate p4 = criteriaBuilder.and(p1, p2);
			criteriaQuery.where(criteriaBuilder.and(p3, p4));
			criteriaQuery.orderBy(criteriaBuilder.asc(rootBroker
					.get(Broker_.pigeonsCreated)));
			brokerList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (brokerList == null || brokerList.isEmpty()) {
				return null;
			} else {
				return brokerList;
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public List<Broker> getBrokers(long agentId) {
		List<Broker> brokerList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Broker> criteriaQuery = criteriaBuilder
					.createQuery(Broker.class);
			Root<Broker> rootBroker = criteriaQuery.from(Broker.class);
			criteriaQuery.select(rootBroker);
			criteriaQuery.where(criteriaBuilder.equal(
					rootBroker.get(Broker_.agentId), agentId));
			brokerList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (brokerList == null || brokerList.isEmpty()) {
				return null;
			} else {
				return brokerList;
			}
		} finally {
			entityManager.close();
		}
	}
}