package com.bizlers.pigeons.api.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PigeonServerUserConfig {

	private static final String SCHEME = "scheme";

	private static final String REGION = "region";

	private static final String PIGEON_SERVER_HOST = "pigeonServerHost";

	private static final String PIGEON_SERVER_PORT = "pigeonServerPort";

	private static final String INCOMING_MESSAGE_PROCESSING_THREAD = "incomingMessageProcesssingThreads";

	private static final String KEYSTORE_PATH = "keyStorePath";

	private static final String KEYSTORE_PASSWORD = "keyStorePassword";

	private static final String TRUSTSTORE_PATH = "trustStorePath";

	private static final String TRUSTSTORE_PASSWORD = "trustStorePassword";

	private static final String PRIVATE_KEY_PASSWORD = "privateKeyPassword";

	private static final String ACKNOWLEGMENT_TIMEOUT = "acknowlegmentTimeout";

	private static final String BROKER_KEYSTORE_FILE = "brokerKeyStoreFile";

	private static final String BROKER_KEYSTORE_PASSWORD = "brokerKeyStorePassword";

	private static final String TLS_VERSION = "tlsVersion";

	private static final String MEMORY_STORAGE = "memoryStorage";

	private long userId;

	private String sessionId;

	private String region;

	private String scheme;

	private String pigeonServerHost;

	private int pigeonServerPort;

	private String brokerKeystorePath;

	private String brokerKeystorePassword;

	private String privateKeyPassword;

	private String trustStorePassword;

	private String trustStorePath;

	private String keyStorePassword;

	private String keyStorePath;

	private String tlsVersion;

	private int acknowlegmentTimeout;

	private long messageCount;

	private boolean memoryStorage;

	private int arrivedMessageProcesssingThreads;

	public PigeonServerUserConfig(PigeonServerUserConfig config) {
		this.userId = config.userId;
		this.sessionId = config.sessionId;
		this.region = config.region;
		this.scheme = config.scheme;
		this.pigeonServerHost = config.pigeonServerHost;
		this.pigeonServerPort = config.pigeonServerPort;
		this.brokerKeystorePath = config.brokerKeystorePath;
		this.brokerKeystorePassword = config.brokerKeystorePassword;
		this.privateKeyPassword = config.privateKeyPassword;
		this.trustStorePassword = config.trustStorePassword;
		this.trustStorePath = config.trustStorePath;
		this.keyStorePassword = config.keyStorePassword;
		this.keyStorePath = config.keyStorePath;
		this.tlsVersion = config.tlsVersion;
		this.acknowlegmentTimeout = config.acknowlegmentTimeout;
		this.messageCount = config.messageCount;
		this.memoryStorage = config.memoryStorage;
		this.arrivedMessageProcesssingThreads = config.arrivedMessageProcesssingThreads;
	}

	private PigeonServerUserConfig(Builder builder) {
		this.userId = builder.userId;
		this.sessionId = builder.sessionId;
		this.region = builder.region;
		this.scheme = builder.scheme;
		this.pigeonServerHost = builder.pigeonServerHost;
		this.pigeonServerPort = builder.pigeonServerPort;
		this.brokerKeystorePath = builder.brokerKeystorePath;
		this.brokerKeystorePassword = builder.brokerKeystorePassword;
		this.privateKeyPassword = builder.privateKeyPassword;
		this.trustStorePassword = builder.trustStorePassword;
		this.trustStorePath = builder.trustStorePath;
		this.keyStorePassword = builder.keyStorePassword;
		this.keyStorePath = builder.keyStorePath;
		this.tlsVersion = builder.tlsVersion;
		this.acknowlegmentTimeout = builder.acknowlegmentTimeout;
		this.messageCount = builder.messageCount;
		this.memoryStorage = builder.memoryStorage;
		this.arrivedMessageProcesssingThreads = builder.arrivedMessageProcesssingThreads;
	}

	public long getUserId() {
		return userId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getScheme() {
		return scheme;
	}

	public String getRegion() {
		return region;
	}

	public String getPigeonServerHost() {
		return pigeonServerHost;
	}

	public int getPigeonServerPort() {
		return pigeonServerPort;
	}

	public String getBrokerKeystorePath() {
		return brokerKeystorePath;
	}

	public String getBrokerKeystorePassword() {
		return brokerKeystorePassword;
	}

	public String getPrivateKeyPassword() {
		return privateKeyPassword;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public String getTrustStorePath() {
		return trustStorePath;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public String getKeyStorePath() {
		return keyStorePath;
	}

	public String getTlsVersion() {
		return tlsVersion;
	}

	public int getAcknowlegmentTimeout() {
		return acknowlegmentTimeout;
	}

	public long getMessageCount() {
		return messageCount;
	}

	public boolean isMemoryStorage() {
		return memoryStorage;
	}

	public int getArrivedMessageProcesssingThreads() {
		return arrivedMessageProcesssingThreads;
	}

	public static class Builder {

		private long userId;

		private String sessionId;

		private String scheme;

		private String region;

		private String pigeonServerHost;

		private int pigeonServerPort;

		private boolean memoryStorage;

		private String brokerKeystorePath;

		private String brokerKeystorePassword;

		private String tlsVersion;

		private int acknowlegmentTimeout;

		private long messageCount;

		private int arrivedMessageProcesssingThreads;

		private String privateKeyPassword;

		private String trustStorePassword;

		private String trustStorePath;

		private String keyStorePassword;

		private String keyStorePath;

		public Builder() {

		}

		public static Builder createFromFile(String configFilePath)
				throws PigeonServerException {
			Properties properties = new Properties();
			InputStream inputStream = Thread.currentThread()
					.getContextClassLoader()
					.getResourceAsStream(configFilePath);
			try {
				properties.load(inputStream);
			} catch (IOException e) {
				throw new PigeonServerException("Error accessing config file.",
						e);
			}
			String brokerKeystoreFile = properties
					.getProperty(BROKER_KEYSTORE_FILE);
			String brokerKeystorePassword = properties
					.getProperty(BROKER_KEYSTORE_PASSWORD);
			String tlsVersion = properties.getProperty(TLS_VERSION);
			boolean memoryStorage = Boolean.parseBoolean(properties
					.getProperty(MEMORY_STORAGE));
			String scheme = properties.getProperty(SCHEME);
			String region = properties.getProperty(REGION);
			String pigeonServerHost = properties
					.getProperty(PIGEON_SERVER_HOST);
			int pigeonServerPort = Integer.parseInt(properties
					.getProperty(PIGEON_SERVER_PORT));
			int arrivedMessageProcesssingThreads = Integer.parseInt(properties
					.getProperty(INCOMING_MESSAGE_PROCESSING_THREAD));
			String keyStorePath = properties.getProperty(KEYSTORE_PATH);
			String trustStorePath = properties.getProperty(TRUSTSTORE_PATH);
			String keyStorePassword = properties.getProperty(KEYSTORE_PASSWORD);
			String trustStorePassword = properties
					.getProperty(TRUSTSTORE_PASSWORD);
			String privateKeyPassword = properties
					.getProperty(PRIVATE_KEY_PASSWORD);
			int acknowlegmentTimeout = Integer.parseInt(properties
					.getProperty(ACKNOWLEGMENT_TIMEOUT));

			Builder builder = new Builder();
			builder.serverScheme(scheme);
			builder.region(region);
			builder.serverHost(pigeonServerHost);
			builder.serverPort(pigeonServerPort);
			builder.brokerKeystorePath(brokerKeystoreFile);
			builder.brokerKeystorePassword(brokerKeystorePassword);
			builder.tlsVersion(tlsVersion);
			builder.memoryStorage(memoryStorage);
			builder.arrivedMessageProcesssingThreads(arrivedMessageProcesssingThreads);
			builder.keyStorePath(keyStorePath);
			builder.keyStorePassword(keyStorePassword);
			builder.trustStorePath(trustStorePath);
			builder.trustStorePassword(trustStorePassword);
			builder.privateKeyPassword(privateKeyPassword);
			builder.acknowlegmentTimeout(acknowlegmentTimeout);
			return builder;
		}

		public PigeonServerUserConfig build() {
			return new PigeonServerUserConfig(this);
		}

		public Builder userId(long userId) {
			this.userId = userId;
			return this;
		}

		public Builder sessionId(String sessionId) {
			this.sessionId = sessionId;
			return this;
		}

		public Builder serverScheme(String scheme) {
			this.scheme = scheme;
			return this;
		}

		public Builder region(String region) {
			this.region = region;
			return this;
		}

		public Builder serverHost(String pigeonServerHost) {
			this.pigeonServerHost = pigeonServerHost;
			return this;
		}

		public Builder serverPort(int pigeonServerPort) {
			this.pigeonServerPort = pigeonServerPort;
			return this;
		}

		public Builder brokerKeystorePath(String brokerKeystorePath) {
			this.brokerKeystorePath = brokerKeystorePath;
			return this;
		}

		public Builder brokerKeystorePassword(String brokerKeystorePassword) {
			this.brokerKeystorePassword = brokerKeystorePassword;
			return this;
		}

		public Builder tlsVersion(String tlsVersion) {
			this.tlsVersion = tlsVersion;
			return this;
		}

		public Builder memoryStorage(boolean memoryStorage) {
			this.memoryStorage = memoryStorage;
			return this;
		}

		public Builder acknowlegmentTimeout(int acknowlegmentTimeout) {
			this.acknowlegmentTimeout = acknowlegmentTimeout;
			return this;
		}

		public Builder messageCount(long messageCount) {
			this.messageCount = messageCount;
			return this;
		}

		public Builder arrivedMessageProcesssingThreads(
				int arrivedMessageProcesssingThreads) {
			this.arrivedMessageProcesssingThreads = arrivedMessageProcesssingThreads;
			return this;
		}

		public Builder trustStorePassword(String trustStorePassword) {
			this.trustStorePassword = trustStorePassword;
			return this;
		}

		public Builder trustStorePath(String trustStorePath) {
			this.trustStorePath = trustStorePath;
			return this;
		}

		public Builder keyStorePassword(String keyStorePassword) {
			this.keyStorePassword = keyStorePassword;
			return this;
		}

		public Builder keyStorePath(String keyStorePath) {
			this.keyStorePath = keyStorePath;
			return this;
		}

		public Builder privateKeyPassword(String privateKeyPassword) {
			this.privateKeyPassword = privateKeyPassword;
			return this;
		}
	}
}
