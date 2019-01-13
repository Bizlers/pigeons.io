package com.bizlers.pigeons.api.server.persistent.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.bizlers.pigeons.api.server.internal.MessageStore;
import com.bizlers.pigeons.api.server.persistent.models.MessageInfo;
import com.bizlers.pigeons.api.server.persistent.models.MessageInfo_;

/**
 * @author saurabh
 * 
 */
public class DBMessageStore implements MessageStore {
	private static EntityManagerFactory factory = EntityManagerFactoryProvider
			.getEntityManagerFactory();

	@Override
	public void insert(MessageInfo message) {
		EntityManager entityManager = null;
		try {
			entityManager = factory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.merge(message);
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
			MessageInfo message = entityManager.getReference(MessageInfo.class, id);
			if (message != null) {
				entityManager.remove(message);
			}
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}

	@Override
	public void delete(long currentTime, long interval) {
		EntityManager entityManager = null;
		try {
			entityManager = factory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.createNativeQuery(
					"UPDATE message_info SET state = 1 where timestamp < " + (currentTime - interval))
					.executeUpdate();
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}

	@Override
	public List<MessageInfo> get(long currentTime, long interval) {
		EntityManager entityManager = null;
		List<MessageInfo> messageList = null;
		try {
			entityManager = factory.createEntityManager();
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<MessageInfo> criteriaQuery = criteriaBuilder.createQuery(MessageInfo.class);
			Root<MessageInfo> messageInfoRoot = criteriaQuery.from(MessageInfo.class);
			criteriaQuery.select(messageInfoRoot);
			Predicate p1 = criteriaBuilder.lessThan(messageInfoRoot.get(MessageInfo_.timestamp),
					(currentTime - interval));
			Predicate p2 = criteriaBuilder.notEqual(messageInfoRoot.get(MessageInfo_.state), MessageInfo.STATE_DELETED);
			criteriaQuery.where(criteriaBuilder.and(p1, p2));
			messageList = entityManager.createQuery(criteriaQuery).getResultList();
			if (messageList == null || messageList.isEmpty()) {
				return null;
			} else {
				return messageList;
			}
		} finally {
			entityManager.close();
		}
	}

	@Override
	public MessageInfo get(long id) {
		EntityManager entityManager = null;
		MessageInfo message = null;
		try {
			entityManager = factory.createEntityManager();
			message = entityManager.find(MessageInfo.class, id);
		} finally {
			entityManager.close();
		}
		return message;
	}

	public void update(MessageInfo messageInfo) {
		EntityManager entityManager = null;
		try {
			entityManager = factory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.merge(messageInfo);
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}
	
	@Override
	public void delete(long id, boolean physical) {
		if(physical) {
			delete(id);
		} else {
			MessageInfo messageInfo = get(id);
			messageInfo.setState(MessageInfo.STATE_DELETED);
			update(messageInfo);
		}
	}
}
