package com.bizlers.pigeons.api.server.support.persistent.dao;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.bizlers.pigeons.api.server.support.persistent.models.PigeonUsage;
import com.jcabi.aspects.Loggable;

public class PigeonUsageStore {
	private static EntityManagerFactory factory = EntityManagerFactoryProvider
			.getEntityManagerFactory();

	@Loggable(value = Loggable.DEBUG)
	public void createPigeonUsage(PigeonUsage pigeonUsage) {
		EntityManager entityManager = null;
		pigeonUsage.setRetrievedTime(new java.util.Date());

		try {
			entityManager = factory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.persist(pigeonUsage);
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void updatePigeonUsage(PigeonUsage pigeonUsage) {
		EntityManager entityManager = null;
		pigeonUsage.setRetrievedTime(new java.util.Date());

		try {
			entityManager = factory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.merge(pigeonUsage);
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public PigeonUsage getPigeonUsage(long userId) {
		EntityManager entityManager = null;
		try {
			entityManager = factory.createEntityManager();
			return entityManager.find(PigeonUsage.class, userId);
		} finally {
			entityManager.close();
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void deletePigeonUsage(PigeonUsage pigeonUsage) {
		EntityManager entityManager = null;
		try {
			entityManager = factory.createEntityManager();
			entityManager.getTransaction().begin();
			PigeonUsage managedPigeonUsage = entityManager.find(
					PigeonUsage.class, pigeonUsage.getUserId());
			if (managedPigeonUsage != null) {
				entityManager.remove(managedPigeonUsage);
			}
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}

}
