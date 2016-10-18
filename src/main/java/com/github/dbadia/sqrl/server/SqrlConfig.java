package com.github.dbadia.sqrl.server;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.dbadia.sqrl.server.backchannel.SqrlNutToken;

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
	public enum ImageFormat {
		PNG, JPG
	}

	/**
	 * The amount of time the SQRL "Nut" will be valid for. That is the maximum amount of time that can pass between us
	 * (server) generating the QR code and us receiving the clients response
	 *
	 * It is strongly recommended that this value be set to 15 minutes (default) or more as this is a new protocol. The
	 * user may use SQRl quite infrequently at first and my need time to recall their SQRL password, remember how it
	 * works etc.
	 */
	@XmlElement
	private int nutValidityInSeconds = (int) TimeUnit.MINUTES.toSeconds(15);

	@XmlElement
	private ImageFormat qrCodeFileType = ImageFormat.PNG;

	/**
	 * REQUIRED: The path (full URL or partial URI) of the backchannel servlet which the SQRL client will call
	 */
	@XmlElement
	private String backchannelServletPath;

	/**
	 * The SQRL Server Friendly Name; optional, if not provided will be set the hostname of the site
	 */
	@XmlElement
	private String serverFriendlyName;

	@XmlTransient
	private SecureRandom secureRandom;

	/**
	 * REQUIRED: The 16 byte AES key used to encrypt {@link SqrlNutToken}
	 */
	@XmlElement
	private byte[] aesKeyBytes;

	/**
	 * A list of one or more headers (X-Forwarded-For, etc) from which to get the users real IP. SQRL requires the users
	 * real IP to respond to the client correctly. The headers will be checked in the order given.
	 */
	@XmlElement
	private String[] ipForwardedForHeaders;

	@XmlElement
	private String sqrlPersistenceFactoryClass = "com.github.dbadia.sqrl.server.data.SqrlJpaPersistenceFactory";

	/**
	 * The cookie name to use for the SQRL correlator during authentication
	 */
	@XmlElement
	private String correlatorCookieName = "sqrlcorrelator";

	/**
	 * The full classname of the {@link ClientAuthStateUpdater} that will push status updates to the client browser
	 */
	@XmlElement
	private String clientAuthStateUpdaterClass = null;

	/**
	 * The frequency with which to execute {@link SqrlPersistence#cleanUpExpiredEntries()} via {@link java.util.Timer};
	 * set to -1 to disable completely
	 */
	@XmlElement
	private int cleanupTaskExecInMinutes = 15;

	/**
	 * The cookie name to use for the SQRL first nut during authentication
	 */
	@XmlElement
	private String firstNutCookieName = "sqrlfirstnut";

	public String[] getIpForwardedForHeaders() {
		return ipForwardedForHeaders;
	}

	public void setIpForwardedForHeaders(final String[] ipForwardedForHeaders) {
		this.ipForwardedForHeaders = ipForwardedForHeaders;
	}

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

	public long getCurrentTimeMs() {
		return System.currentTimeMillis();
	}

	public String getSqrlPersistenceFactoryClass() {
		return sqrlPersistenceFactoryClass;
	}

	public String getCorrelatorCookieName() {
		return correlatorCookieName;
	}

	public String getFirstNutCookieName() {
		return firstNutCookieName;
	}

	public void setFirstNutCookieName(final String firstNutCookieName) {
		this.firstNutCookieName = firstNutCookieName;
	}

	public void setSqrlPersistenceFactoryClass(final String sqrlPersistenceFactoryClass) {
		this.sqrlPersistenceFactoryClass = sqrlPersistenceFactoryClass;
	}

	public void setCorrelatorCookieName(final String correlatorCookieName) {
		this.correlatorCookieName = correlatorCookieName;
	}

	public int getCleanupTaskExecInMinutes() {
		return cleanupTaskExecInMinutes;
	}

	public void setCleanupTaskExecInMinutes(final int cleanupTaskExecInMinutes) {
		this.cleanupTaskExecInMinutes = cleanupTaskExecInMinutes;
	}

	public String getClientAuthStateUpdaterClass() {
		return clientAuthStateUpdaterClass;
	}

	public void setClientAuthStateUpdaterClass(final String clientAuthStateUpdaterClass) {
		this.clientAuthStateUpdaterClass = clientAuthStateUpdaterClass;
	}
}
