package com.bizlers.pigeons.dao;

import java.util.List;

import com.bizlers.pigeons.models.Agent;
import com.bizlers.pigeons.utils.PigeonServerException;

public interface AgentDAO {
	public void createAgent(Agent agent);

	public void updateAgent(Agent agent);

	public Agent getAgent(String agentName, String ip)
			throws PigeonServerException;

	public Agent getAgent(long agentId);

	public List<Agent> getAgent(String ip) throws PigeonServerException;

	public void removeAgent(Agent agent);

}