package com.bizlers.pigeons.dao;

import java.util.List;

import com.bizlers.pigeons.models.Broker;
import com.bizlers.pigeons.utils.PigeonServerException;

public interface BrokerDAO {
	public void createBroker(Broker broker);

	public List<Broker> getBrokers(long agentId);

	public Broker getBrokerByName(String brokerName)
			throws PigeonServerException;

	public Broker updateBroker(Broker broker);

	public void removeBroker(Broker broker);

	public List<Broker> getBrokers(String type, String region,
			boolean isMaxLimit, String status);

	public int getBrokerCount(String type) throws PigeonServerException;

	public Broker getBroker(long brokerId);

	public Broker getBroker(String type, String region, String status);

	public Broker getBroker(String type, String status);

	public Broker getBroker(String ip, int port) throws PigeonServerException;

	public List<Broker> getBrokers(String type, boolean isMaxLimit,
			String status);

	public List<Broker> getAgentBrokersForPigeonGeneration(long agentId);

	public Broker getRedirectBroker(String ip, int redirectPort)  throws PigeonServerException;

}