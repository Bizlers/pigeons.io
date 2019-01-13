package com.bizlers.pigeons.agent.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;

import com.bizlers.pigeons.agent.core.AgentConfigurator;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

import expectj.ExpectJ;
import expectj.ExpectJException;
import expectj.Spawn;
import expectj.TimeoutException;

public class SystemInformation {

	private static float totalRAM = 0;

	@Loggable(value = Loggable.DEBUG)
	public static boolean isPortBusy(int port) {
		ServerSocket socket = null;
		try {
			sleep();
			socket = new ServerSocket(port);
			return false;
		} catch (IOException e) {
			return true;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
	}

	@Loggable(value = Loggable.DEBUG)
	public static int getProcessId(Process process)
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {
		int pid = 0;
		Field field = process.getClass().getDeclaredField("pid");
		field.setAccessible(true);
		pid = field.getInt(process);
		return pid;
	}

	public static int getProcessId(int port) {
		Spawn sh = null;
		ExpectJ ex;
		ex = new ExpectJ(10);
		try {
			sh = ex.spawn("/bin/bash");
			sleep();
			sh.send("fuser -vn tcp  " + port + "\n");
			sleep();
			String string = sh.getCurrentStandardOutContents();
			if (string != null && !string.isEmpty()) {
				return Integer.parseInt(string.trim());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				sh.expectClose();
			} catch (ExpectJException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	@Loggable(value = Loggable.DEBUG)
	public static int getProcessId2(AgentConfigurator config, int port)
			throws InterruptedException, IOException, ExpectJException,
			TimeoutException {
		ExpectJ expectinator = new ExpectJ(100);
		Spawn shell = expectinator.spawn("/bin/bash");
		sleep();
		String configFilePath = config.getAgentInstallationDir()
				+ "/config/config_" + port + ".conf";
		String mosquittoBinaryPath = config.getMqttServerInstallationDir()
				+ "mosquitto";
		String command = "ps -ef | grep " + configFilePath + " | grep "
				+ mosquittoBinaryPath + " | awk 'NR==3{ print $2 }'";
		shell.send(command + "\n");
		sleep();
		String pid = shell.getCurrentStandardOutContents();
		shell.stop();
		shell.expectClose();
		if (pid.length() > 0)
			return Integer.parseInt(pid.trim());
		return 0;
	}

	@Loggable(value = Loggable.DEBUG)
	public static float getProcessMemoryConsumption(int processId)
			throws InterruptedException, IOException, ExpectJException,
			TimeoutException {
		ExpectJ expectinator = new ExpectJ(100);
		Spawn shell = expectinator.spawn("/bin/bash");
		sleep();
		String command = "ps -p " + processId
				+ " -o %mem | awk 'NR==2{ print $1 }'";
		shell.send(command + "\n");
		sleep();
		String memory = shell.getCurrentStandardOutContents();
		shell.stop();
		shell.expectClose();
		if (memory.length() > 0)
			return Float.parseFloat(memory.trim());
		return 0;
	}

	@Loggable(value = Loggable.DEBUG)
	public static float getTotalMemory() throws InterruptedException,
			IOException, ExpectJException, TimeoutException {
		if (totalRAM == 0) {
			ExpectJ expectinator = new ExpectJ(100);
			Spawn shell = expectinator.spawn("/bin/bash");
			sleep();
			String command = "cat /proc/meminfo | grep MemTotal | awk '{ print $2 }'";
			shell.send(command + "\n");
			sleep();
			String memory = shell.getCurrentStandardOutContents();
			shell.stop();
			shell.expectClose();
			if (memory.length() > 0) {
				totalRAM = Integer.parseInt(memory.trim());
				return Integer.parseInt(memory.trim());
			}
			return 0;
		}
		return totalRAM;
	}

	private static void sleep() {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			Logger.error(SystemInformation.class, "Error in sleep");
		}
	}
}
