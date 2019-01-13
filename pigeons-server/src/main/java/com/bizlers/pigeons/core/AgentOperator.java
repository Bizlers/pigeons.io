package com.bizlers.pigeons.core;

import org.hibernate.StaleObjectStateException;
import org.springframework.retry.annotation.Retryable;

import com.bizlers.pigeons.models.Agent;
import com.bizlers.pigeons.utils.PigeonServerException;

public interface AgentOperator {
	
	public static final int MAX_ATTEMPT = 10;

	public void createAgent(Agent agent) throws PigeonServerException;

	@Retryable(maxAttempts = MAX_ATTEMPT, include = {StaleObjectStateException.class})
	public void removeAgent(String agentname, String ip) throws PigeonServerException;

	@Retryable(maxAttempts = MAX_ATTEMPT, include = {StaleObjectStateException.class})
	public void updateAgent(Agent agent) throws PigeonServerException;

	public Agent readAgent(long agentId);

	public Agent readAgent(String agentName, String ip) throws PigeonServerException;

}
