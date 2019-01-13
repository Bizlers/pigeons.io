package com.bizlers.pigeons.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;

@Component
public class ContextListener implements ServletContextListener {

	private static PigeonsRecovery pigeonsRecovery;
	
	@Autowired
	private PigeonNotifier pigeonNotifier;

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		pigeonNotifier.terminate();
		pigeonsRecovery.terminateScheduler();
		Logger.info(this, "Authorization context has been destroyed");
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		Logger.info(this, "Authorization context initialized");
		loadVersionInfo(event.getServletContext());
		loadPortInfo(event.getServletContext());
	}

	/**
	 * Loads application version data into ServletContext
	 */
	@Loggable(Loggable.TRACE)
	private void loadVersionInfo(ServletContext context) {
		Logger.debug(this, "Loading application version data");
		InputStream inputStream = context.getResourceAsStream("/META-INF/MANIFEST.MF");
		try {
			Manifest manifest = new Manifest(inputStream);
			Attributes attributes = manifest.getMainAttributes();
			String buildVersion = attributes.getValue(Name.SPECIFICATION_VERSION);
			String buildNumber = attributes.getValue(Name.IMPLEMENTATION_VERSION);
			String appTitle = attributes.getValue(Name.SPECIFICATION_TITLE);
			String buildDate = attributes.getValue("Build-Time");
			String buildEnv = attributes.getValue("Build-Environment");
			if (buildEnv == null) {
				buildEnv = "DEVELOP";
			}
			Logger.info(this, "%s version: %s.%s. Date %s", appTitle, buildVersion, buildNumber, buildDate);
			Logger.info(this, "Build Environment %s", buildEnv);

			context.setAttribute("build.version", buildVersion);
			context.setAttribute("build.date", buildDate);
			context.setAttribute("build.env", buildEnv);
			context.setAttribute("build.number", buildNumber);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Loggable(Loggable.TRACE)
	private static void loadPortInfo(ServletContext context) {
		Logger.debug(ContextListener.class, "Reading port.properties file");
		InputStream inputStream = Thread.currentThread()
				.getContextClassLoader().getResourceAsStream("port.properties");
		Properties properties = new Properties();

		try {
			properties.load(inputStream);
		} catch (IOException e) {
			Logger.error(ContextListener.class,
					"Error loading port properties. Exception %[exception]s", e);
		}
		String sessionPort = properties.getProperty("sessionPort");
		String agentPort = properties.getProperty("agentPort");
		String applicationPort = properties.getProperty("applicationPort");
		String userPort = properties.getProperty("userPort");

		if (sessionPort != null) {
			Logger.info(ContextListener.class, "Session Port : %s", sessionPort);
			context.setAttribute("sessionPort", sessionPort);
		}
		if (agentPort != null) {
			Logger.info(ContextListener.class, "Agent Port : %s", agentPort);
			context.setAttribute("agentPort", agentPort);
		}
		if (applicationPort != null) {
			Logger.info(ContextListener.class, "Application Port : %s",
					applicationPort);
			context.setAttribute("applicationPort", applicationPort);
		}
		if (userPort != null) {
			Logger.info(ContextListener.class, "user Port : %s", userPort);
			context.setAttribute("userPort", userPort);
		}

	}
}
