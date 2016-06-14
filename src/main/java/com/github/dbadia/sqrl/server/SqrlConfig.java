package com.github.dbadia.sqrl.server;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

// TODO: doc required and optional
// @formatter:off
/**
 * Bean which stores our server-side SQRL configuration settings. 
 * <p/>
 * <b>Required</b>fields to be set are:
 * <ul>
 * <li>{@link #aesKeyBytes} - </li>
 * <li>{@link #backchannelServletPath}
 * <li>the third item
 * </ul><p>
 *  <b>Recommended</b> fields to be set are:
 * 
 * @author Dave Badia
 *
 */
//@formatter:on
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SqrlConfig {
	public static enum ImageFormat {
		PNG, JPG
	}

	/**
	 * The amount of time the SQRL "Nut" will be valid for. That is the maximum amount of time that can pass between us
	 * (server) generating the QR code and us receiving the clients response
	 * 
	 * TODO: what is a reasonable default? This is a new protocol so they may need time to read about it, download and
	 * install app, etc
	 */
	@XmlElement
	private int nutValidityInSeconds = (int) TimeUnit.MINUTES.toMillis(5);

	@XmlElement
	private ImageFormat qrCodeFileType = ImageFormat.PNG;
	/**
	 * The path (full URL or partial URI) of the backchannel servlet which the SQRL client will call
	 */
	@XmlElement
	private String backchannelServletPath;

	/**
	 * The Server Friendly Name
	 */
	@XmlElement
	private String serverFriendlyName;

	@XmlTransient
	private SecureRandom secureRandom;

	@XmlElement
	private byte[] aesKeyBytes;


	public int getNutValidityInSeconds() {
		return nutValidityInSeconds;
	}

	/**
	 * Set the length of time (in seconds) that the nut will be valid for
	 * 
	 * @param nutValidityInSeconds
	 * @throws IllegalArgumentException
	 *             if nutValidityInSeconds is less than 0
	 */
	public void setNutValidityInSeconds(final int nutValidityInSeconds) {
		if (nutValidityInSeconds < 0) {
			throw new IllegalArgumentException("nutValidityInSeconds must be greater than zero");
		}
		this.nutValidityInSeconds = nutValidityInSeconds;
	}

	public ImageFormat getQrCodeFileType() {
		return qrCodeFileType;
	}

	public void setQrCodeFileType(final ImageFormat qrCodeFileType) {
		this.qrCodeFileType = qrCodeFileType;
	}

	public SecureRandom getSecureRandom() {
		return secureRandom;
	}

	public void setSecureRandom(final SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
	}

	public void setAESKeyBytes(final byte[] aesKeyBytes) {
		this.aesKeyBytes = aesKeyBytes;
	}

	public byte[] getAESKeyBytes() {
		return aesKeyBytes;
	}

	public String getBackchannelServletPath() {
		return backchannelServletPath;
	}

	/**
	 * Required: sets the URL to the servlet endpoint which will handle SQRL client requests, can be either a full URL
	 * or a URI for example: http://127.0.0.1:8080/sqrlbc /sqrlbc
	 * 
	 * If the value is a partial URI ("/sqrlbc" or "sqrlbc")), then the request URL will be used as the
	 * protocol/host/port TODO: example
	 * 
	 * @param backchannelServletPath
	 *            the servlet endpoint which will handle SQRL client requests, for example: http://127.0.0.1:8080/sqrlbc
	 */
	public void setBackchannelServletPath(final String backchannelServletPath) {
		this.backchannelServletPath = backchannelServletPath;
	}

	public String getServerFriendlyName() {
		return serverFriendlyName;
	}

	public void setServerFriendlyName(final String serverFriendlyName) {
		this.serverFriendlyName = serverFriendlyName;
	}
}
