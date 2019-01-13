package com.bizlers.pigeons.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bizlers.pigeons.models.Broker;
import com.bizlers.pigeons.models.Pigeon;
import com.bizlers.pigeons.utils.PigeonServerException;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

/**
 * @author saurabh
 * 
 */
@Component
@Scope("singleton")
public class PigeonGenerator {

	private static final int MINALLOCATION = 10;
	private static int PIGEON_MAXLIMIT = 1000;
	
	@Autowired
	private PigeonOperator pigeonOperator;
	
	@Autowired
	private BrokerOperator brokerOperator;
	
	@Autowired
	private PigeonDistributer pigeonDistributer;

	@Loggable(value = Loggable.DEBUG)
	public int createPigeons(Broker broker, int allocationCount)
			throws PigeonServerException {
		int i = 0;
		if ((broker.getMaxLimit() - broker.getPigeonsCreated()) < allocationCount)
			allocationCount = (broker.getMaxLimit() - broker
					.getPigeonsCreated());
		if (allocationCount == 0)
			return 0;
		for (i = 0; i < allocationCount; i++) {
			Pigeon pigeon = new Pigeon(broker.getIp(), broker.getPort(),
					broker.getRegion(), broker.getBrokerType(),
					broker.getBrokerName(), broker.getAgentId(),
					broker.getRedirectPort());

			if (broker.getBrokerType().equalsIgnoreCase(Broker.TYPE_L0)) {
				pigeon.setClientId(broker.getBrokerName() + "/"
						+ broker.getBrokerName());
			} else {
				if (broker.getConnectedTo() != null
						&& broker.getConnectedTo().size() != 0) {
					pigeon.setClientId(broker.getConnectedTo().get(0)
							.getBrokerName()
							+ "/" + broker.getBrokerName());
				} else {
					throw new PigeonServerException(
							"Broker connections cannot be empty for pigeon creation.");
				}
			}
			pigeonOperator.createPigeon(pigeon);
		}
		broker.setPigeonsCreated(broker.getPigeonsCreated() + i);
		brokerOperator.updateBroker(broker.getId(), 0, i);
		return i;
	}

	@Loggable(value = Loggable.DEBUG)
	public void generatePublishPigeons(int pigeonCount, String region) {
		try {
			List<Broker> brokerList = brokerOperator.getBrokerList(
					Broker.TYPE_L0, region, true, Broker.STATUS_ONLINE);
			generatePigeons(pigeonCount, brokerList);
		} catch (PigeonServerException e) {
			Logger.error(
					this,
					"Failed to generate publish pigeons. Exception : %[exception]s",
					e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void generatePigeons(int pigeonCount, String region) {
		try {
			List<Broker> brokerList = brokerOperator.getBrokerList(
					Broker.TYPE_L2, region, true, Broker.STATUS_ONLINE);
			generatePigeons(pigeonCount, brokerList);
		} catch (PigeonServerException e) {
			Logger.error(this,
					"Failed to generate pigeons. Exception : %[exception]s", e);
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public void generatePigeons(int pigeonCount, List<Broker> brokerList)
			throws PigeonServerException {
		int pigeonsTobeGenerated, processingCounter = 0, minAllocation = MINALLOCATION, totalPigeonsGenerated = 0;
		int brokerProccessed = 0, brokerPigeonsCount = 0;
		if (brokerList != null && brokerList.size() > 0) {
			pigeonsTobeGenerated = pigeonCount * brokerList.size();
		} else {
			return;
		}
		while (totalPigeonsGenerated < pigeonsTobeGenerated
				&& brokerProccessed < brokerList.size()) {
			while (brokerList.size() > processingCounter) {
				brokerPigeonsCount = brokerList.get(processingCounter).getPigeonsCreated();
				if (brokerPigeonsCount < PIGEON_MAXLIMIT) {
					if ((PIGEON_MAXLIMIT - brokerPigeonsCount) < MINALLOCATION) {
						totalPigeonsGenerated = totalPigeonsGenerated
								+ (createPigeons(brokerList.get(processingCounter),
										(PIGEON_MAXLIMIT - brokerPigeonsCount)));
					} else {
						totalPigeonsGenerated = totalPigeonsGenerated
								+ (createPigeons(brokerList.get(processingCounter),
										minAllocation));
					}
				} else {
					brokerProccessed++;
				}
				if (totalPigeonsGenerated >= pigeonsTobeGenerated) {
					break;
				}
				processingCounter++;
			}
			if (brokerList.size() == processingCounter && brokerProccessed <= brokerList.size()) {
				processingCounter = 0;
			}
		}
	}

	public void generateAgentPigeons(long agentId, int pigeonCount) {
		new AgentPigeonGenerator(agentId, pigeonCount).start();
	}

	private class AgentPigeonGenerator extends Thread {
		private long agentId;
		private int pigeonCount;

		public AgentPigeonGenerator(long agentId, int pigeonCount) {
			this.agentId = agentId;
			this.pigeonCount = pigeonCount;
		}

		public void run() {
			try {
				generatePigeons(pigeonCount,
						brokerOperator.getAgentBrokers(agentId));
			} catch (PigeonServerException e) {
				Logger.error(this,
						"Failed to create pigeons. Exception : %[exception]s",
						e);
			}
			pigeonDistributer.distributePigeons(agentId);
		}
	}
}
