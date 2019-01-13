package com.bizlers.pigeons.api.server.persistent.dao;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.bizlers.pigeons.api.server.internal.PigeonStore;
import com.bizlers.pigeons.api.server.persistent.models.PigeonInfo;

public class DBPigeonStore implements PigeonStore {

	private static EntityManagerFactory factory = EntityManagerFactoryProvider
			.getEntityManagerFactory();

	@Override
	public void insert(PigeonInfo pigeon) {
		EntityManager entityManager = null;
		try {
			entityManager = factory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.merge(pigeon);
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}

	@Override
	public void update(PigeonInfo pigeon) {
		EntityManager entityManager = null;
		try {
			entityManager = factory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.merge(pigeon);
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}

	@Override
	public void delete(long id) {
		EntityManager entityManager = null;
		try {
			entityManager = factory.createEntityManager();
			entityManager.getTransaction().begin();
			PigeonInfo pigeon = entityManager.find(PigeonInfo.class, id);
			if (pigeon != null) {
				entityManager.remove(pigeon);
			}
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}

	@Override
	public PigeonInfo get(long id) {
		PigeonInfo pigeon = null;
		EntityManager entityManager = null;
		try {
			entityManager = factory.createEntityManager();
			pigeon = entityManager.find(PigeonInfo.class, id);
		} finally {
			entityManager.close();
		}
		return pigeon;
	}
}
