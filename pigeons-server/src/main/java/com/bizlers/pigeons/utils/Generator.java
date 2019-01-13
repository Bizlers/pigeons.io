package com.bizlers.pigeons.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Random;

import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;
import com.sun.jersey.core.util.Base64;

/**
 * @author saurabh
 * 
 */
public class Generator {

	private static String Bank = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	@Loggable(value = Loggable.DEBUG)
	public static byte[] getHash(byte[] s, String type) {
		MessageDigest md;
		byte[] encoded = null;
		try {
			md = MessageDigest.getInstance(type);
			md.update(s);
			encoded = md.digest();
			encoded = getBase64(encoded);
		} catch (NoSuchAlgorithmException e) {
			Logger.warn(Generator.class, "Exception : %[exception]s", e);
		}
		return (encoded);
	}

	@Loggable(value = Loggable.DEBUG)
	public static byte[] getBase64(byte[] s) {
		return Base64.encode(s);
	}

	@Loggable(value = Loggable.DEBUG)
	public static String getUserNamePassword() {
		String key = "";
		int i = 0;
		Random rand = new Random();// Random.class
		for (i = 0; i < 73; i++) {
			key = key + Bank.charAt(rand.nextInt(Bank.length()));
		}
		return (key + getTime());
	}

	@Loggable(value = Loggable.DEBUG)
	public static String getUniqueName() {
		String key = "";
		int i = 0;
		Random rand = new Random();// Random.class
		for (i = 0; i < 4; i++) {
			key = key + Bank.charAt(rand.nextInt(Bank.length()));
		}
		return key;
	}

	@Loggable(value = Loggable.DEBUG)
	public static String getClientId() {
		String clientId = "";
		int i = 0;
		Random rand = new Random();// Random.class
		for (i = 0; i < 10; i++) {
			clientId = clientId + Bank.charAt(rand.nextInt(Bank.length()));
		}
		return clientId;
	}

	@Loggable(value = Loggable.DEBUG)
	private static String getTime() {
		java.util.Date date = new java.util.Date();
		String time = (new Timestamp(date.getTime())).toString();
		time = time.replace("-", "");
		time = time.replace(":", "");
		time = time.replace(".", "");
		time = time.replace(" ", "");
		return time;
	}
}
