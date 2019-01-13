package com.bizlers.pigeons.agent.core;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.bizlers.pigeons.agent.utils.SystemInformation;
import com.bizlers.pigeons.commommodels.Broker;
import com.jcabi.log.Logger;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

import expectj.ExpectJException;
import expectj.TimeoutException;

/*
 * Scheduler that will run at every TIME_INTERVAL interval to
 * 1) Start all unexpectedly terminated brokers.
 * 2) Check SLB connection and recover in case not connected.
 * 3) Send all pigeons pending for connect update to pigeons server.
 * 4) Send all pigeons pending for reallocation to pigeons server.
 */
public class RecoveryScheduler {

	public static final int TIME_INTERVAL = 120;

	private Timer timer;

	private boolean running = false;

	private AgentService agentService;

	public RecoveryScheduler(AgentService agentService) {
		timer = new Timer();
		timer.schedule(new RecoveryTask(), TIME_INTERVAL * 1000,
				TIME_INTERVAL * 1000);
		this.agentService = agentService;
	}

	public void terminateScheduler() {
		timer.cancel();
	}

	public boolean isRunning() {
		return running;
	}

	class RecoveryTask extends TimerTask {
		public void run() {
			// Start new Thread for Processing Recovery Task.
			new ProcessRecoveryTask().start();
		}
	}

	private class ProcessRecoveryTask extends Thread {
		public void run() {
			try {
				// Start all unexpectedly terminated brokers.
				startStoppedBrokers();

				// Check SLB connection and recover in case not connected.
				AgentMonitor.INSTANCE.checkConnection();

				running = true;
				agentService.reallocateExpiredPigons();
				processPigeonsPendingForConnectUpdate();
				processPigeonsPendingForReallocationUpdate();
				running = false;

				processTempPigeonsPendingForConnectUpdate();
				processTempPigeonsPendingForReallocationUpdate();
			} catch (PigeonAgentException | InterruptedException | IOException
					| ExpectJException | TimeoutException e) {
				Logger.warn(this, "Exception in Recovery Timer. %[exception]s",
						e);
			}
		}

	}

	private void startStoppedBrokers() throws PigeonAgentException,
			InterruptedException, IOException, ExpectJException,
			TimeoutException {
		Set<Integer> ports = agentService.getBrokerAccessControllers().keySet();
		for (Integer port : ports) {
			int processId = SystemInformation.getProcessId(port);
			if (processId != 0) {
				float memoryConsumption = SystemInformation
						.getProcessMemoryConsumption(processId);
				float totalRAM = SystemInformation.getTotalMemory();
				if (memoryConsumption != 0 && totalRAM != 0) {
					float memoryUsed = ((memoryConsumption / 100) * totalRAM) / 1024;
					if (memoryUsed >= 50) {
						Logger.warn(
								this,
								"Broker at port %d is consuming %f MB memory so restarting broker...",
								port, memoryUsed);
						Runtime.getRuntime().exec("kill -1 " + processId);
						Thread.sleep(1000);
						Runtime.getRuntime().exec("kill -9 " + processId);
						processId = restartBroker(port);
					} else {
						Logger.info(this,
								"Broker at port %d is consuming %f MB memory.",
								port, memoryUsed);
					}
				}
			}
			if (processId != 0) {
				BrokerAccessController accessController = agentService
						.getBrokerAccessController(port);
				if (accessController != null) {
					if (accessController.getProcessId() == processId) {
						Logger.info(this,
								"Broker at port %d is already running...", port);
					} else if (accessController.getProcessId() != processId) {
						Logger.warn(
								this,
								"Failed to start broker on port %d. Port has been occupied by another process. PID: %d",
								port, processId);
					} else {
						Logger.info(this, "Invalid case for port %d ", port);
					}
				} else {
					Logger.warn(
							this,
							"BrokerAccessController is null for  port %d. creating new BrokerAccessController ",
							port);
					agentService.createBrokerAccessController(port, processId);

				}
			} else {
				Logger.warn(this,
						"Broker at port %d is down. Restarting broker...", port);
				restartBroker(port);
			}
		}
	}

	private int restartBroker(int port) {
		int processId = 0;
		Broker broker = new Broker();
		broker.setPort(port);
		try {
			agentService.startBroker(broker, null);
			processId = SystemInformation.getProcessId(port);
			if (processId > 0) {
				Logger.info(this,
						"Broker at port %d has been restarted successfully...",
						port);
			} else {
				Logger.info(this, "Broker at port %d cannot be restarted...",
						port);
			}
		} catch (PigeonAgentException e) {
			Logger.warn(this,
					"Failed to restart broker at port: %d. %[exception]s",
					port, e);
		}
		return processId;
	}

	/*
	 * Send all pigeons pending for connect update to pigeons server.
	 */
	private void processPigeonsPendingForConnectUpdate()
			throws PigeonAgentException {
		for (Iterator<String> iterator = agentService
				.getPigeonsPendingForConnectUpdate().iterator(); iterator
				.hasNext();) {
			String[] params = iterator.next().split(":");
			String clientId = params[0];
			int port = Integer.parseInt(params[1]);
			int clientAPIVersion = Integer.parseInt(params[2]);
			try {
				agentService.clientConnected(clientId, port, clientAPIVersion,
						true);
				iterator.remove();
			} catch (UniformInterfaceException | ClientHandlerException
					| PigeonAgentException e) {
				Logger.warn(
						this,
						"Failed to update pigeon on pigeons server to be online. ClientId: %s",
						clientId);
			}
		}
	}

	/*
	 * Send all pigeons pending for reallocation to pigeons server.
	 */
	private void processPigeonsPendingForReallocationUpdate()
			throws PigeonAgentException {
		for (Iterator<String> iterator = agentService
				.getPigeonsPendingForReallocationUpdate().iterator(); iterator
				.hasNext();) {
			String[] params = iterator.next().split(":");
			String clientId = params[0];
			int port = Integer.parseInt(params[1]);
			try {
				agentService.reallocatePigeon(clientId, port, true);
				iterator.remove();
			} catch (UniformInterfaceException | ClientHandlerException
					| PigeonAgentException e) {
				Logger.warn(this, "Failed to reallocate pigeon. ClientId: %s",
						clientId);
			}
		}
	}

	/*
	 * Add pigeons connected while existing pigeonsPendingForConnectUpdate list
	 * was in process, to pigeonsPendingForConnectUpdate.
	 */
	private void processTempPigeonsPendingForConnectUpdate()
			throws PigeonAgentException {
		agentService.getPigeonsPendingForConnectUpdate().addAll(
				agentService.getTempPigeonsPendingForConnectUpdate());
		agentService.getTempPigeonsPendingForConnectUpdate().clear();
	}

	/*
	 * Add pigeons connected while existing pigeonsPendingForReallocationUpdate
	 * list was in process, to pigeonsPendingForReallocationUpdate.
	 */
	private void processTempPigeonsPendingForReallocationUpdate()
			throws PigeonAgentException {
		agentService.getPigeonsPendingForReallocationUpdate().addAll(
				agentService.getTempPigeonsPendingForReallocationUpdate());
		agentService.getTempPigeonsPendingForReallocationUpdate().clear();
	}
}
