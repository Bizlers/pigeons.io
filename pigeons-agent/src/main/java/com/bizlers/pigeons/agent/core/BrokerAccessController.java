package com.bizlers.pigeons.agent.core;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.jcabi.log.Logger;

public class BrokerAccessController {

	private static final Object TAG_CLASS_NAME = BrokerAccessController.class
			.getName();

	// Absolute path to the mosquitto's password util file (mosquitto_passwd)
	private String mosquittoPasswd;

	// Absolute path to the password file of current broker
	private String passwordFile;

	// Process ID of the current process
	private int processId;

	// Port of the current process
	private int port;

	private ExecutorService executor;

	public BrokerAccessController(String mosquittoPasswd, String passwordFile,
			int port, int processId) {
		this.mosquittoPasswd = mosquittoPasswd;
		this.passwordFile = passwordFile;
		this.port = port;
		this.processId = processId;

		executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "Broker Access Controller - "
						+ BrokerAccessController.this.port);
			}
		});
	}

	public int getProcessId() {
		return processId;
	}

	public void setProcessId(int processId) {
		this.processId = processId;
	}

	public void add(BrokerAccessControlCallback callback, String clientId,
			String userName, String password) {
		executor.execute(new AddTask(callback, clientId, userName, password));
	}

	public void remove(BrokerAccessControlCallback callback, String clientId,
			String userName) {
		executor.execute(new RemoveTask(callback, clientId, userName));
	}

	public void reload(BrokerAccessControlCallback callback,
			String reloadAction, String userName, String password) {
		executor.execute(new ReloadTask(callback, reloadAction, userName,
				password));
	}

	private class AddTask implements Runnable {

		private BrokerAccessControlCallback callback;
		private String clientId;
		private String userName;
		private String password;

		public AddTask(BrokerAccessControlCallback callback, String clientId,
				String userName, String password) {
			this.callback = callback;
			this.clientId = clientId;
			this.userName = userName;
			this.password = password;
		}

		@Override
		public void run() {
			try {
				addToPasswrodFile(userName, password);
				callback.onSuccessfullyAdded(clientId, port);
			} catch (IOException | InterruptedException e) {
				Logger.warn(TAG_CLASS_NAME,
						"Failed to create user. Port: %d, UserName: %s", port,
						userName);
			}
		}
	}

	private class RemoveTask implements Runnable {

		private BrokerAccessControlCallback callback;
		private String clientId;
		private String userName;

		public RemoveTask(BrokerAccessControlCallback callback,
				String clientId, String userName) {
			this.callback = callback;
			this.clientId = clientId;
			this.userName = userName;
		}

		@Override
		public void run() {
			try {
				removeFromPasswordFile(userName);
				callback.onSuccessfullyRemoved(clientId, port);
			} catch (IOException | InterruptedException e) {
				Logger.warn(TAG_CLASS_NAME,
						"Failed to delete user. Port: %d, UserName: ", port,
						userName);
			}
		}
	}

	private class ReloadTask implements Runnable {

		private BrokerAccessControlCallback callback;
		private String reloadAction;
		private String userName;
		private String password;

		public ReloadTask(BrokerAccessControlCallback callback,
				String reloadAction, String userName, String password) {
			this.callback = callback;
			this.reloadAction = reloadAction;
			this.userName = userName;
			this.password = password;
		}

		@Override
		public void run() {
			Logger.info(
					AgentConfigurator.class,
					"Reloading configuration. Port: %d, ProcessId %d, Reload Action: %s",
					port, processId, reloadAction);
			if (processId > 1) {
				for (int i = 0; i < 5; i++) {
					try {
						reloadConfiguration(processId);
						if (callback.verifyReload(reloadAction, port, userName,
								password)) {
							callback.onReloadCompletion(reloadAction, port);
						} else {
							Logger.warn(
									this,
									"Failed to reload configuration. Port: %d, ProcessId: %d, Retry Count: %d, Reload Action: %s",
									port, processId, i, reloadAction);
						}
					} catch (IOException | InterruptedException e) {
						Logger.warn(
								this,
								"Exception while reloading configuration. Port: %d, ProcessId: %d, Retry Count: %d, Reload Action: %s",
								port, processId, i, reloadAction);
					}
				}
			} else {
				Logger.warn(
						this,
						"Failed to reload configuration as ProcessId is not greater than 1. Port: %d, ProcessId: %d, Reload Action: %s",
						port, processId, reloadAction);
			}
		}
	}

	private void addToPasswrodFile(String userName, String password)
			throws InterruptedException, IOException {
		String command = mosquittoPasswd + " -b " + passwordFile + " "
				+ userName + " " + password;
		Runtime.getRuntime().exec(command).waitFor();
	}

	private void removeFromPasswordFile(String userName)
			throws InterruptedException, IOException {
		String command = mosquittoPasswd + " -D " + passwordFile + " "
				+ userName;
		Runtime.getRuntime().exec(command).waitFor();
	}

	private void reloadConfiguration(int processId)
			throws InterruptedException, IOException {
		Runtime.getRuntime().exec("kill -1 " + processId).waitFor();
	}

	public interface BrokerAccessControlCallback {

		public void onSuccessfullyAdded(String clientId, int port);

		public void onSuccessfullyRemoved(String clientId, int port);

		public void onReloadCompletion(String reloadAction, int port);

		public boolean verifyReload(String reloadAction, int port,
				String userName, String password);
	}
}
