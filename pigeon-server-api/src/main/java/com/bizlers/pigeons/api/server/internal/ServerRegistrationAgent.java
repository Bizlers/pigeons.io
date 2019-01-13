package com.bizlers.pigeons.api.server.internal;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.MediaType;

import com.bizlers.pigeons.api.server.PigeonServerException;
import com.bizlers.pigeons.api.server.PigeonServerUserConfig;
import com.bizlers.pigeons.tools.appreg.Application;
import com.bizlers.utils.jaxrs.SecuredClientBuilder;
import com.jcabi.log.Logger;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public enum ServerRegistrationAgent {
	INSTANCE;

	private static final Map<PigeonServerUserConfig, Application> appMap = new ConcurrentHashMap<>();

	private static final String URL_APPLICATIONS_PATH = "pigeons-server/rest/applications";

	public synchronized Application register(PigeonServerUserConfig config) throws PigeonServerException {
		Application application = null;
		if (appMap.containsKey(config)) {
			return appMap.get(config);
		} else {
			application = registerOnServer(config);
			appMap.put(config, application);
		}
		return application;
	}
	
	private Application registerOnServer(PigeonServerUserConfig config) throws PigeonServerException {
		long userId = config.getUserId();
		String url = "https://" + config.getPigeonServerHost() + ":" + config.getPigeonServerPort() + "/"
				+ URL_APPLICATIONS_PATH + "?userId=" + config.getUserId();
		Client client;
		try {
			client = new SecuredClientBuilder().keyStorePath(config.getKeyStorePath())
					.trustStorePath(config.getTrustStorePath()).keyStorePassword(config.getKeyStorePassword())
					.trustStorePassword(config.getTrustStorePassword())
					.privateKeyPassword(config.getPrivateKeyPassword()).build();
			client.addFilter(new HTTPBasicAuthFilter(String.valueOf(config.getUserId()), config.getSessionId()));
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			Status status = response.getClientResponseStatus();
			if (status == ClientResponse.Status.OK) {
				Application application = response.getEntity(Application.class);
				if (application == null) {
					throw new PigeonServerException("The application value is null.");
				}
				return application;
			} else if (status == ClientResponse.Status.NOT_FOUND) {
				Application application = new Application();
				application.setAccountId(userId);
				webResource = client.resource(url);
				response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, application);
				if (response.getClientResponseStatus() != ClientResponse.Status.CREATED) {
					Logger.error(this, "HTTP error code: %d", response.getStatus());
					throw new PigeonServerException(
							"Failed to register the application." + response.getClientResponseStatus());
				} else {
					Logger.info(this, "Sucessfully registered the application. ");
					return application;
				}
			} else {
				Logger.error(this, "HTTP error code: %d", response.getStatus());
				throw new PigeonServerException(
						"Failed to retrieve the application. " + response.getClientResponseStatus());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
