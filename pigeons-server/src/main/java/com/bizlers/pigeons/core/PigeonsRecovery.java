package com.bizlers.pigeons.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

@Component
public class PigeonsRecovery {
	
	private Timer timer = new Timer();

	private int recoveryInterval = 600;
	
	@Autowired
	private PigeonDistributer pigeonDistributor;

	@PostConstruct
	public void init() {
		loadRecoveryInterval();
		timer.schedule(new PigeonsRecoveryTask(), recoveryInterval * 1000,
				recoveryInterval * 1000);
	}

	private void loadRecoveryInterval() {
		Logger.debug(ContextListener.class, "Reading pigeons.properties file");
		InputStream inputStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("pigeons.properties");
		Properties properties = new Properties();
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			Logger.error(ContextListener.class, "Error loading pigeons properties. Exception %[exception]s", e);
		}
		String pigeonsRecoveryInterval = properties.getProperty("pigeonsRecoveryInterval");
		Logger.info(ContextListener.class, "pigeonsRecoveryInterval : %s", pigeonsRecoveryInterval);
		recoveryInterval = Integer.parseInt(pigeonsRecoveryInterval);
	}
	
	public void terminateScheduler() {
		timer.cancel();
	}

	/*
	 * Get all the pigeons that are in alloted state for more than 15 minutes
	 * and destroy them.
	 */
	class PigeonsRecoveryTask extends TimerTask {

		@Loggable(value = Loggable.INFO, limit = 10, unit = TimeUnit.SECONDS)
		public void run() {
			Logger.debug(this, "Destroying expired pigeons");
			pigeonDistributor.destroyPigeons(recoveryInterval);
		}
	}
}
