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

import com.bizlers.pigeons.models.Application;
import com.bizlers.pigeons.models.Application_;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.jcabi.aspects.Loggable;

@Repository
public class ApplicationDAOImp implements ApplicationDAO {

	@Qualifier("pigeonsServerEmf")
	@PersistenceContext(unitName="com.bizlers.pigeons")
	private EntityManager entityManager;

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void create(Application application) {
		try {
			entityManager.persist(application);
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Application get(long appId) throws PigeonServerException {
		Application application = null;
		try {
			application = entityManager.find(Application.class, appId);
			if (application != null
					&& application.getState() == Application.DEACTIVATED) {
				throw new PigeonServerException("Application Deactivated");
			}
		} finally {
			entityManager.close();
		}
		return application;
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Application getByAccountId(long accountId) {
		List<Application> applicationList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Application> criteriaQuery = criteriaBuilder
					.createQuery(Application.class);
			Root<Application> root = criteriaQuery.from(Application.class);
			criteriaQuery.select(root);
			Predicate p1 = criteriaBuilder.equal(
					root.get(Application_.accountId), accountId);
			Predicate p2 = criteriaBuilder.notEqual(
					root.get(Application_.state), Application.DEACTIVATED);
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			applicationList = entityManager.createQuery(criteriaQuery)
					.setMaxResults(1).getResultList();
			if (applicationList == null || applicationList.isEmpty()) {
				return null;
			} else {
				return applicationList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Loggable(value = Loggable.DEBUG)
	public Application get(String emailId) {
		List<Application> applicationList = null;
		try {
			CriteriaBuilder criteriaBuilder = entityManager
					.getCriteriaBuilder();
			CriteriaQuery<Application> criteriaQuery = criteriaBuilder
					.createQuery(Application.class);
			Root<Application> root = criteriaQuery.from(Application.class);
			criteriaQuery.select(root);
			Predicate p1 = criteriaBuilder.equal(
					root.get(Application_.emailId), emailId);
			Predicate p2 = criteriaBuilder.notEqual(
					root.get(Application_.state), Application.DEACTIVATED);
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			applicationList = entityManager.createQuery(criteriaQuery)
					.setMaxResults(1).getResultList();
			if (applicationList == null || applicationList.isEmpty()) {
				return null;
			} else {
				return applicationList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void update(Application application) {
		try {
			entityManager.merge(application);
		} finally {
			entityManager.close();
		}
	}

	@Override
	@Transactional("pigeonServerTxManager")
	@Loggable(value = Loggable.DEBUG)
	public void delete(Application application) {
		try {
			entityManager.remove(application);
		} finally {
			entityManager.close();
		}
	}
}
