package com.bizlers.pigeons.core;

import java.util.List;

import org.hibernate.StaleObjectStateException;
import org.springframework.retry.annotation.Retryable;

import com.bizlers.pigeons.models.Broker;
import com.bizlers.pigeons.utils.PigeonServerException;

public interface BrokerOperator {
	
	public static final int MAX_ATTEMPT = 10;

	public void createBroker(Broker broker) throws PigeonServerException;

	public Broker readBroker(String brokername) throws PigeonServerException;

	public List<Broker> getAgentBrokers(long agentId);

	@Retryable(maxAttempts = MAX_ATTEMPT, include = {StaleObjectStateException.class})
	public void removeBroker(String brokername) throws PigeonServerException;

	@Retryable(maxAttempts = MAX_ATTEMPT, include = {StaleObjectStateException.class})
	public void updateBroker(long brokerId, int alloted, int pigeonCreated) throws PigeonServerException;

	@Retryable(maxAttempts = MAX_ATTEMPT, include = {StaleObjectStateException.class})
	public void updateBroker(String brokerName, int alloted, int pigeonCreated) throws PigeonServerException;

	@Retryable(maxAttempts = MAX_ATTEMPT, include = {StaleObjectStateException.class})
	public void updateBroker(Broker broker) throws PigeonServerException;

	public List<Broker> getBrokerList(String type, String region, boolean isMaxLimit, String status)
			throws PigeonServerException;

	public Broker readBroker(String ip, int port) throws PigeonServerException;

	public Broker getBroker(String type, String region, String status) throws PigeonServerException;

	public Broker getBroker(String type, String status) throws PigeonServerException;

}
