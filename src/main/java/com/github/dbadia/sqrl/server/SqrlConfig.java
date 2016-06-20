package com.github.dbadia.sqrl.server;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

// @formatter:off
/**
 * Bean which stores our server-side SQRL configuration settings. 
 * <p/>
 * <b>Required</b>fields to be set are:
 * <ul>
 * <li>{@link #aesKeyBytes} - </li>
 * <li>{@link #backchannelServletPath}
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
	 * It is strongly recommended that this value be set to 15 minutes ore more as this is a new protocol. The user may
	 * use SQRl quite infrequently at first and my need time to recall their SQRL password, remember how it works etc.
	 */
	@XmlElement
	private int nutValidityInSeconds = (int) TimeUnit.MINUTES.toMillis(15);

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

	// @formatter:off
	/**
	 * Required: sets the URL to the servlet endpoint which will handle SQRL client requests, can be either a full URL,
	 * a full URI, or a partial URI.
	 * <p/>
	 * 
	 * <table summary="Backchannel Servlet Path Examples">
	 *   <tr>
	 *      <td>Setting</td><td>Login URL</td><td>Computed BC url</td>
	 *   </tr>
	 *   <tr>
	 *      <td>https://sqrljava.tech/sqrlexample/sqrlbc</td><td>https://sqrljava.tech/sqrlexample/login</td><td>https://sqrljava.tech/sqrlexample/sqrlbc</td>
	 *   </tr>
	 *   <tr>
	 *      <td>/sqrl/sqrlbc</td><td>https://sqrljava.tech/myapp/login</td><td>https://sqrljava.tech/<b>sqrl/sqrlbc</b></td>
	 *   </tr>
	 *   <tr>
	 *      <td>sqrlbc</td><td>https://sqrljava.tech/sqrlexample/login</td><td>https://sqrljava.tech/sqrlexample/<b>sqrlbc</b></td>
	 *   </tr>
	 * </table>
	 * 
	 * @param backchannelServletPath
	 *            the servlet endpoint which will handle SQRL client requests.  Can be a full URL,
	 * a full URI, or a partial URI
	 */
	// @formatter:on
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
