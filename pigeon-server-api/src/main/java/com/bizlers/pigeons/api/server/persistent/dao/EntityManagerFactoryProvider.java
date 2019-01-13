package com.bizlers.pigeons.api.server.persistent.dao;

/**
 * @author saurabh
 *
 */
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.jcabi.aspects.Loggable;

public class EntityManagerFactoryProvider {

	private static EntityManagerFactory emf = null;

	@Loggable(Loggable.DEBUG)
	public static EntityManagerFactory getEntityManagerFactory() {
		if (emf == null) {
			emf = Persistence
					.createEntityManagerFactory("com.bizlers.pigeons.server.api");
		}
		return emf;
	}
}