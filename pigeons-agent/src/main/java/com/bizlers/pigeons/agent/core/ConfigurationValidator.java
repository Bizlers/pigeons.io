package com.bizlers.pigeons.agent.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.bizlers.pigeons.agent.utils.SystemInformation;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

import expectj.ExpectJ;
import expectj.ExpectJException;
import expectj.Spawn;
import expectj.TimeoutException;

public class ConfigurationValidator {

	@Loggable(value = Loggable.DEBUG)
	public static void validate(AgentConfigurator config)
			throws PigeonAgentException, IOException, InterruptedException,
			ExpectJException, TimeoutException {
		validatePorts(config);
		validateFolderStructure(config);
		validateBrokers(config);
		validateExpectAvailability(config);
	}

	@Loggable(value = Loggable.DEBUG)
	public static void validatePorts(AgentConfigurator config)
			throws PigeonAgentException {
		List<Integer> portList = new ArrayList<Integer>();
		if (config.isStartL0() == true) {
			portList.addAll(config.getBrokerPortArrayListL0());
		}
		if (config.isStartL1() == true) {
			portList.addAll(config.getBrokerPortArrayListL1());
		}
		if (config.isStartL2() == true) {
			portList.addAll(config.getBrokerPortArrayListL2());
		}
		portList.add(config.getStatusListenerBrokerPort());

		List<Integer> busyPortList = new ArrayList<>();
		for (Integer port : portList) {
			if (SystemInformation.isPortBusy(port)) {
				busyPortList.add(port);
			}
		}
		if (!busyPortList.isEmpty()) {
			Logger.error(ConfigurationValidator.class,
					" Given Ports are busy : %s", busyPortList);
			throw new PigeonAgentException(
					" Ports mentioned in the configuration are already occupied. try with other ports.");
		}
		if ((!config.isRedirectPortAvailable() && config
				.isRedirectDestinationPortAvailable())
				|| (config.isRedirectPortAvailable() && !config
						.isRedirectDestinationPortAvailable())) {
			Logger.error(
					ConfigurationValidator.class,
					" Redirect Port and Redirect Destination Port should be mention in configuration file.");
			throw new PigeonAgentException(
					" Redirect Port and Redirect Destination Port should be mention in configuration file.");
		}

		if (config.isRedirectDestinationPortAvailable()
				&& !config.getBrokerPortArrayListL2().contains(
						config.getRedirectDestinationPort())) {
			Logger.error(ConfigurationValidator.class,
					" Redirect Destination Port should be in L2 brokers : %d",
					config.getRedirectPort());
			throw new PigeonAgentException(
					" Redirect Destination Port should be in L2 broker ports.");
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public static void validateFolderStructure(AgentConfigurator config)
			throws PigeonAgentException {
		try {
			File file = new File(config.getAgentInstallationDir());
			if (!file.isDirectory()) {
				file.mkdirs();
			}
			file = new File(config.getAgentInstallationDir() + "/config");
			if (!file.isDirectory()) {
				file.mkdirs();
			}
			file = new File(config.getAgentInstallationDir() + "/logs");
			if (!file.isDirectory()) {
				file.mkdirs();
			}
			file = new File(config.getAgentInstallationDir()
					+ "/config/defaultConfig.conf");

			file = new File("./target/classes/defaultConfig.conf");
			if (file.exists()) {
				Runtime.getRuntime().exec(
						"cp " + "./target/classes/defaultConfig.conf "
								+ config.getAgentInstallationDir() + "/config");
			} else {
				file = new File(config.getAgentInstallationDir()
						+ "/Mosquitto/defaultConfig.conf");
				if (file.exists()) {
					Runtime.getRuntime().exec(
							"cp " + config.getAgentInstallationDir()
									+ "/Mosquitto/defaultConfig.conf "
									+ config.getAgentInstallationDir()
									+ "/config");
				} else {
					throw new PigeonAgentException(
							" defaultConfig.conf cannot be found in Mosquitto directory in agent installation path. please check.");
				}
			}
		} catch (IOException e) {
			throw new PigeonAgentException(
					" defaultConfig.conf cannot be found. please check.");
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public static void validateBrokers(AgentConfigurator config)
			throws PigeonAgentException, InterruptedException,
			ExpectJException, TimeoutException {
		FileWriter fileWriter = null;
		File file = new File(config.getAgentInstallationDir() + "/temp.conf");
		try {
			if (!file.exists()) {
				file.createNewFile();
				fileWriter = new FileWriter(file, true);
				BufferedWriter out = new BufferedWriter(fileWriter);
				out.write("user " + config.getUser());
				out.close();
				fileWriter.close();
			}
			Runtime.getRuntime().exec(
					config.getMqttServerInstallationDir() + "mosquitto -c "
							+ file.getAbsolutePath() + " -p "
							+ config.getStatusListenerBrokerPort());
			sleep(5);
			if (SystemInformation.isPortBusy(config
					.getStatusListenerBrokerPort())) {
				String command = "ps -ef | grep "
						+ config.getMqttServerInstallationDir() + "mosquitto "
						+ " | grep " + config.getStatusListenerBrokerPort()
						+ " | awk 'NR==1{ print $2 }'";
				ExpectJ expectinator = new ExpectJ(10);
				Spawn shell = expectinator.spawn("/bin/bash");
				shell.send(command + "\n");
				sleep(5);
				String output = shell.getCurrentStandardOutContents();
				int pid = Integer.parseInt(output.trim());
				System.out.println("Process Id : " + pid);
				if (pid != 0) {
					Runtime.getRuntime().exec("kill -9 " + pid);
				} else {
					Logger.info(ConfigurationValidator.class,
							" PID cannot be equal to zero");
				}
				shell.stop();
				shell.expectClose();
			} else {
				throw new PigeonAgentException(
						"Mosquitto cannot be started from given directory : "
								+ config.getMqttServerInstallationDir());
			}
		} catch (IOException e) {
			throw new PigeonAgentException(
					"Mosquitto cannot be started from given directory : "
							+ config.getMqttServerInstallationDir(), e);
		} finally {
			file.delete();
		}
	}

	@Loggable(value = Loggable.DEBUG, limit = 5, unit = TimeUnit.SECONDS)
	public static void validateExpectAvailability(AgentConfigurator config)
			throws PigeonAgentException {
		FileWriter fileWriter = null;
		File file = new File(config.getAgentInstallationDir() + "/temp.exp");
		try {
			if (file.exists()) {
				file.delete();
			}

			file.createNewFile();
			fileWriter = new FileWriter(file, true);
			BufferedWriter out = new BufferedWriter(fileWriter);
			out.write("send ExpectJValidationSuccessfull\n");
			out.close();
			fileWriter.close();

			ExpectJ expectinator = new ExpectJ(100);
			Spawn shell = expectinator.spawn("/bin/bash");
			String command = "expect  " + file.getAbsolutePath() + "\n";
			shell.send(command);
			sleep(5);
			String output = shell.getCurrentStandardOutContents();
			shell.stop();
			shell.expectClose();
			if (!output.equalsIgnoreCase("ExpectJValidationSuccessfull")) {
				throw new PigeonAgentException(
						"Expect is not installed on your machine. Kindly install it.");
			}
		} catch (IOException | ExpectJException | TimeoutException e) {
			throw new PigeonAgentException(
					"Expect is not installed on your machine. Kindly install it.");
		} finally {
			file.delete();
		}
	}

	private static void sleep(int count) {
		try {
			Thread.sleep(200 * count);
		} catch (InterruptedException e) {
			Logger.error(ConfigurationValidator.class, "Error in sleep");
		}
	}
}
