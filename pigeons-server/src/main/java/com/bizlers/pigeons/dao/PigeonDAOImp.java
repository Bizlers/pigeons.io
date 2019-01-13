package com.bizlers.pigeons.dao;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.bizlers.pigeons.models.Broker;
import com.bizlers.pigeons.models.Broker_;
import com.bizlers.pigeons.models.Pigeon;
import com.bizlers.pigeons.models.Pigeon_;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

/**
 * @author saurabh
 * 
 */
@Repository
public class PigeonDAOImp implements PigeonDAO {

	@Qualifier("pigeonsServerEmf")
	@PersistenceContext(unitName="com.bizlers.pigeons")
	private EntityManager entityManager;

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void activatePigeons(String clientId) {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			criteriaQuery.where(criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.clientId), clientId));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.setLockMode(LockModeType.PESSIMISTIC_WRITE)
					.getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return;
			}
			for (int i = 0; i < pigeonList.size(); i++) {
				pigeonList.get(i).setLastUpdated(new java.util.Date());
				pigeonList.get(i).setStatus(Pigeon.STATUS_READY);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPigeonbyClientId(String clientId)
			throws PigeonServerException {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			Predicate p1 = (criteriaBuilder.notEqual(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_DELETED));
			Predicate p2 = criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.clientId), clientId);
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.setMaxResults(1).getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return null;
			} else if (pigeonList.size() > 1) {
				throw new PigeonServerException("DUPLICATE RECORDS");
			} else {
				return pigeonList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Pigeon getOldPigeon(String clientId) throws PigeonServerException {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			criteriaQuery.where(criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.clientId), clientId));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.setMaxResults(1).getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return null;
			} else if (pigeonList.size() > 1) {
				throw new PigeonServerException("DUPLICATE RECORDS");
			} else {
				return pigeonList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void createPigeon(Pigeon pigeon) {
		pigeon.setCreated(new java.util.Date());
		pigeon.setLastUpdated(new java.util.Date());
		try {
			entityManager.persist(pigeon);
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void updatePigeon(Pigeon pigeon) {
		pigeon.setLastUpdated(new java.util.Date());
		try {
			entityManager.merge(pigeon);
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public boolean deletePigeon(Pigeon pigeon) {
		boolean isDeleteSuccess = false;
		try {
			Pigeon managedPigeon = entityManager.find(Pigeon.class,
					pigeon.getId());
			if (managedPigeon != null
					&& !managedPigeon.getStatus().equalsIgnoreCase(
							Pigeon.STATUS_DELETED)) {
				managedPigeon.setStatus(Pigeon.STATUS_DELETED);
				entityManager.merge(managedPigeon);
				isDeleteSuccess = true;
			}
		} finally {
			entityManager.close();
		}
		return isDeleteSuccess;
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public List<Pigeon> getCreatedPigeons(String type) {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			Predicate p1 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_CREATED));
			Predicate p2 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.pigeonType), type));
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			criteriaQuery.orderBy(criteriaBuilder.asc(rootPigeon
					.get(Pigeon_.port)));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return null;
			} else {
				return pigeonList;
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public List<Pigeon> getPigeonList(long agentId) {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			Predicate p1 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_CREATED));
			Predicate p2 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.agentId), agentId));
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			criteriaQuery.orderBy(criteriaBuilder.asc(rootPigeon
					.get(Pigeon_.port)));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return null;
			} else {
				return pigeonList;
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void deleteAllPigeonsOfAgent(long agentId) {
		try {
			entityManager.createNativeQuery(
					"update pigeon set pigeon_status = 'D' where agent_id = "
							+ agentId).executeUpdate();
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public int getPigeonCount(String brokerName) {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			Predicate p1 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.brokerName), brokerName));
			Predicate p2 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_READY));
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			criteriaQuery.orderBy(criteriaBuilder.asc(rootPigeon
					.get(Pigeon_.created)));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return 0;
			} else {
				return pigeonList.size();
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPigeon(long appId, String region, int redirectPort,
			boolean isRoundRobinFetch) {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);

			Predicate p1 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.region), region));
			Predicate p2 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_READY));
			Predicate p3 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.pigeonType), Pigeon.TYPE_L2));
			Predicate p4 = criteriaBuilder.and(p1, p2);
			Predicate p5 = criteriaBuilder.and(p3, p4);
			Predicate p6 = criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.redirectPort), redirectPort);
			if (isRoundRobinFetch) {
				Path<String> path = rootPigeon.get(Pigeon_.brokerName);
				Subquery<String> subquery = criteriaQuery
						.subquery(String.class);
				Root<Broker> brokerRoot = subquery.from(Broker.class);
				subquery.select(brokerRoot.get(Broker_.brokerName));
				Predicate p7 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.brokerType), Broker.TYPE_L2);
				Predicate p8 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.status), Broker.STATUS_ONLINE);

				Predicate p9 = criteriaBuilder.greaterThan(
						brokerRoot.get(Broker_.pigeonsCreated),
						brokerRoot.get(Broker_.alloted));
				Predicate p10 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.region), region);
				Predicate p11 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.redirectPort), redirectPort);
				Predicate p12 = criteriaBuilder.and(p7, p8);
				Predicate p13 = criteriaBuilder.and(p10, p11);
				Predicate p14 = criteriaBuilder.and(p12, p9);
				subquery.where(criteriaBuilder.and(p13, p14));
				Predicate p15 = criteriaBuilder.and(p6, p5);
				criteriaQuery.where(criteriaBuilder.and(p15, criteriaBuilder
						.in(path).value(subquery)));
			} else {
				criteriaQuery.where(criteriaBuilder.and(p6, p5));
			}

			criteriaQuery.orderBy(criteriaBuilder.asc(rootPigeon
					.get(Pigeon_.created)));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.setLockMode(LockModeType.PESSIMISTIC_WRITE)
					.setMaxResults(1).getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return null;
			} else {
				pigeonList.get(0).setLastUpdated(new java.util.Date());
				pigeonList.get(0).setStatus(Pigeon.STATUS_ALLOTED);
				pigeonList.get(0).setAppId(appId);
				return pigeonList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPublishPigeon(long appId) {
		Pigeon pigeon = getPublishPigeon(appId, true);
		if (pigeon == null) {
			pigeon = getPublishPigeon(appId, false);
			Logger.warn(this,
					"Retreiving pigeon with isRoundRobinFetch as false.");
		}
		return pigeon;
	}
	
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPublishPigeon(long appId, boolean isRoundRobinFetch) {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			Predicate p1 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_READY));
			Predicate p2 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.pigeonType), Pigeon.TYPE_L0));
			Predicate p3 = (criteriaBuilder.and(p2, p1));
			if (isRoundRobinFetch) {
				Path<String> path = rootPigeon.get(Pigeon_.brokerName);
				Subquery<String> subquery = criteriaQuery
						.subquery(String.class);
				Root<Broker> brokerRoot = subquery.from(Broker.class);
				subquery.select(brokerRoot.get(Broker_.brokerName));
				Predicate p4 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.brokerType), Broker.TYPE_L0);
				Predicate p5 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.status), Broker.STATUS_ONLINE);

				Predicate p6 = criteriaBuilder.greaterThan(
						brokerRoot.get(Broker_.pigeonsCreated),
						brokerRoot.get(Broker_.alloted));
				Predicate p7 = criteriaBuilder.and(p4, p5);
				subquery.where(criteriaBuilder.and(p7, p6));
				criteriaQuery.where(criteriaBuilder.and(p3,
						criteriaBuilder.in(path).value(subquery)));
			} else {
				criteriaQuery.where(p3);
			}
			criteriaQuery.orderBy(criteriaBuilder.asc(rootPigeon
					.get(Pigeon_.created)));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.setLockMode(LockModeType.PESSIMISTIC_WRITE)
					.setMaxResults(1).getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return null;
			} else {
				pigeonList.get(0).setLastUpdated(new java.util.Date());
				pigeonList.get(0).setStatus(Pigeon.STATUS_ALLOTED);
				pigeonList.get(0).setAppId(appId);
				return pigeonList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPublishPigeons(long appId, String brokerName) {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			Predicate p1 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_READY));
			Predicate p2 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.pigeonType), Pigeon.TYPE_L0));
			Predicate p3 = (criteriaBuilder.and(p2, p1));
			Predicate p4 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.brokerName), brokerName));
			criteriaQuery.where(criteriaBuilder.and(p3, p4));
			criteriaQuery.orderBy(criteriaBuilder.asc(rootPigeon
					.get(Pigeon_.created)));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.setLockMode(LockModeType.PESSIMISTIC_WRITE)
					.setMaxResults(1).getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return null;
			} else {
				pigeonList.get(0).setLastUpdated(new java.util.Date());
				pigeonList.get(0).setStatus(Pigeon.STATUS_ALLOTED);
				pigeonList.get(0).setAppId(appId);
				return pigeonList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPublishPigeon(long appId, String region) {
		Pigeon pigeon = getPublishPigeon(appId, region, true);
		if (pigeon == null) {
			pigeon = getPublishPigeon(appId, region, false);
			Logger.warn(
					this,
					"Retreiving pigeon with isRoundRobinFetch as false  region= %s",
					region);
		}
		return pigeon;
	}

	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPublishPigeon(long appId, String region,
			boolean isRoundRobinFetch) {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			Predicate p1 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_READY));
			Predicate p2 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.pigeonType), Pigeon.TYPE_L0));
			Predicate p3 = (criteriaBuilder.and(p2, p1));
			Predicate p4 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.region), region));
			Predicate p5 = criteriaBuilder.and(p4, p3);
			if (isRoundRobinFetch) {
				Path<String> path = rootPigeon.get(Pigeon_.brokerName);
				Subquery<String> subquery = criteriaQuery
						.subquery(String.class);
				Root<Broker> brokerRoot = subquery.from(Broker.class);
				subquery.select(brokerRoot.get(Broker_.brokerName));
				Predicate p6 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.brokerType), Broker.TYPE_L0);
				Predicate p7 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.status), Broker.STATUS_ONLINE);

				Predicate p8 = criteriaBuilder.greaterThan(
						brokerRoot.get(Broker_.pigeonsCreated),
						brokerRoot.get(Broker_.alloted));
				Predicate p9 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.region), region);
				Predicate p10 = criteriaBuilder.and(p6, p7);
				Predicate p11 = criteriaBuilder.and(p9, p10);
				subquery.where(criteriaBuilder.and(p8, p11));
				criteriaQuery.where(criteriaBuilder.and(p5,
						criteriaBuilder.in(path).value(subquery)));
			} else {
				criteriaQuery.where(p5);
			}
			criteriaQuery.orderBy(criteriaBuilder.asc(rootPigeon
					.get(Pigeon_.created)));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.setLockMode(LockModeType.PESSIMISTIC_WRITE)
					.setMaxResults(1).getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return null;
			} else {
				pigeonList.get(0).setLastUpdated(new java.util.Date());
				pigeonList.get(0).setStatus(Pigeon.STATUS_ALLOTED);
				pigeonList.get(0).setAppId(appId);
				return pigeonList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public Pigeon getReplacePigeon(long appId, String clientId) {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			Predicate p1 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_READY));
			Path<String> path = rootPigeon.get(Pigeon_.brokerName);
			Subquery<String> subquery = criteriaQuery.subquery(String.class);
			Root<Pigeon> brokerRoot = subquery.from(Pigeon.class);
			subquery.select(brokerRoot.get(Pigeon_.brokerName));
			Predicate p2 = criteriaBuilder.equal(
					brokerRoot.get(Pigeon_.clientId), clientId);
			subquery.where(p2);
			criteriaQuery.where(criteriaBuilder.and(p1, criteriaBuilder
					.in(path).value(subquery)));
			criteriaQuery.orderBy(criteriaBuilder.asc(rootPigeon
					.get(Pigeon_.created)));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.setLockMode(LockModeType.PESSIMISTIC_WRITE)
					.setMaxResults(1).getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return null;
			} else {
				pigeonList.get(0).setLastUpdated(new java.util.Date());
				pigeonList.get(0).setStatus(Pigeon.STATUS_ALLOTED);
				pigeonList.get(0).setAppId(appId);
				return pigeonList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public Pigeon getPigeon(long appId, int redirectPort, boolean isRoundRobinFetch) {
		List<Pigeon> pigeonList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			Predicate p1 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_READY));
			Predicate p2 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.pigeonType), Pigeon.TYPE_L2));
			Predicate p3 = criteriaBuilder.and(p1, p2);
			Predicate p4 = criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.redirectPort), redirectPort);
			if (isRoundRobinFetch) {
				Path<String> path = rootPigeon.get(Pigeon_.brokerName);
				Subquery<String> subquery = criteriaQuery
						.subquery(String.class);
				Root<Broker> brokerRoot = subquery.from(Broker.class);
				subquery.select(brokerRoot.get(Broker_.brokerName));
				Predicate p5 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.brokerType), Broker.TYPE_L2);
				Predicate p6 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.status), Broker.STATUS_ONLINE);

				Predicate p7 = criteriaBuilder.greaterThan(
						brokerRoot.get(Broker_.pigeonsCreated),
						brokerRoot.get(Broker_.alloted));
				Predicate p8 = criteriaBuilder.equal(
						brokerRoot.get(Broker_.redirectPort), redirectPort);
				Predicate p9 = criteriaBuilder.and(p5, p6);
				Predicate p10 = criteriaBuilder.and(p8, p9);

				subquery.where(criteriaBuilder.and(p7, p10));
				Predicate p11 = criteriaBuilder.and(p4, p3);
				criteriaQuery.where(criteriaBuilder.and(p11, criteriaBuilder
						.in(path).value(subquery)));
			} else {
				criteriaQuery.where(criteriaBuilder.and(p4, p3));
			}
			criteriaQuery.orderBy(criteriaBuilder.asc(rootPigeon
					.get(Pigeon_.created)));
			pigeonList = entityManager.createQuery(criteriaQuery)
					.setLockMode(LockModeType.PESSIMISTIC_WRITE)
					.setMaxResults(1).getResultList();
			if (pigeonList == null || pigeonList.isEmpty()) {
				return null;
			} else {
				pigeonList.get(0).setLastUpdated(new java.util.Date());
				pigeonList.get(0).setStatus(Pigeon.STATUS_ALLOTED);
				pigeonList.get(0).setAppId(appId);
				return pigeonList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public int deleteAllPigeons(String brokerName) {
		int count = 0;
		try {
			count = entityManager.createNativeQuery(
					"delete from pigeon where brokername=" + "'" + brokerName
							+ "' and pigeon_status in ('" + Pigeon.STATUS_READY
							+ "','" + Pigeon.STATUS_CREATED + "')")
					.executeUpdate();
		} finally {
			entityManager.close();
		}
		return count;
	}

	@Override
	public List<Pigeon> getPigeonsToDestroy(int idleTime) {
		List<Pigeon> pigeons = null;
		Calendar calendar = Calendar.getInstance();
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Pigeon> criteriaQuery = criteriaBuilder
					.createQuery(Pigeon.class);
			Root<Pigeon> rootPigeon = criteriaQuery.from(Pigeon.class);
			criteriaQuery.select(rootPigeon);
			Predicate p1 = (criteriaBuilder.equal(
					rootPigeon.get(Pigeon_.status), Pigeon.STATUS_ALLOTED));
			calendar.add(Calendar.SECOND, -idleTime);
			Path<Date> lastUpdated = rootPigeon.get(Pigeon_.lastUpdated);
			Predicate p2 = (criteriaBuilder.lessThan(lastUpdated,
					calendar.getTime()));
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			pigeons = entityManager.createQuery(criteriaQuery).getResultList();
		} finally {
			entityManager.close();
		}
		return pigeons;
	}
}
