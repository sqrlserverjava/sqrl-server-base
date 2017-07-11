package com.github.sqrlserverjava;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.sqrlserverjava.backchannel.SqrlNutToken;
import com.github.sqrlserverjava.enums.SqrlQrCodeImageFormat;
import com.github.sqrlserverjava.persistence.SqrlJpaPersistenceFactory;

// @formatter:off
/**
 * Bean which stores our server-side SQRL configuration settings.
 * Designed to for loaded from an xml config file (see {@link SqrlConfigHelper) or dependency injection 
 * <p/>
 * <b>Required</b>fields to be set are:
 * <ul>
 * <li>{@link #aesKeyBytes}</li>
 * <li>{@link #backchannelServletPath}</li>
 * </ul><p>
 *  <b>Recommended</b> fields to be set are:
 * <li>{@link #clientAuthStateUpdaterClass}</li>
 *
 *  All other fields are optional with sensible defaults
 *
 * @author Dave Badia
 *
 */
//@formatter:on
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SqrlConfig {

	/* *********************************************************************************************/
	/* *************************************** REQUIRED ********************************************/
	/* *********************************************************************************************/
	/**
	 * REQUIRED: The path (full URL or partial URI) of the backchannel servlet which the SQRL clients will call TODO:
	 * examples
	 */
	@XmlElement(required = true)
	private String backchannelServletPath;

	/**
	 * REQUIRED: The base64 encoded 16 byte AES key used to encrypt {@link SqrlNutToken}
	 */
	@XmlElement(required = true)
	private byte[] aesKeyBytes;

	/* *********************************************************************************************/
	/* *************************************** RECOMMENDED ********************************************/
	/* *********************************************************************************************/
	/**
	 * RECOMMENDED: The full classname of the SqrlClientAuthStateUpdater that will push status updates to the client
	 * browser. The sqrl-sever-atomosphere project contains a prebuit updated which uses the atmosphere framework
	 * 
	 * Example: com.github.sqrlserverjava.atmosphere.AtmosphereClientAuthStateUpdater
	 * 
	 * @see {@link SqrlClientAuthStateUpdater}
	 * @see <a href="https://github.com/sqrlserverjava/sqrl-server-atmosphere">sqrl-server-atmosphere</a>
	 */
	@XmlElement(required = false)
	private String clientAuthStateUpdaterClass = null;

	/* *********************************************************************************************/
	/* *************************************** OPTIONAL ********************************************/
	/* *********************************************************************************************/

	/**
	 * The amount of time the SQRL "Nut" will be valid for; default is 15 minutes. That is the maximum amount of time
	 * that can pass between us (server) generating the QR code and us receiving the clients response
	 *
	 * It is strongly recommended that this value be set to 15 minutes (default) or more as this is a new protocol. The
	 * user may use SQRl quite infrequently at first and my need time to recall their SQRL password, remember how it
	 * works etc.
	 */
	@XmlElement(required = false)
	private int nutValidityInSeconds = (int) TimeUnit.MINUTES.toSeconds(15);

	/**
	 * The image format to generate QR codes in; default is PNG
	 */
	@XmlElement(required = false)
	private SqrlQrCodeImageFormat qrCodeFileType = SqrlQrCodeImageFormat.PNG;

	/**
	 * The SQRL Server Friendly Name; default: the hostname of the site
	 */
	@XmlElement(required = false)
	private String serverFriendlyName;

	/**
	 * The secureRandom instance that is used to generate various random tokens; defaults to
	 * {@link SecureRandom#SecureRandom()}. Can only be set via setter
	 * 
	 * @see #setSecureRandom(SecureRandom)
	 */
	@XmlTransient
	private SecureRandom secureRandom;

	/**
	 * A list of one or more comma separated headers (X-Forwarded-For, etc) from which to get the users real IP. SQRL
	 * requires the users real IP to respond to the client correctly. The headers will be checked in the order given.
	 * 
	 * Example: X-Forwarded-For
	 */
	@XmlElement(required = false)
	private String[] ipForwardedForHeaders;

	/**
	 * The SQRL JPA persistence provider class which implements {@link SqrlPersistenceFactory}; defaults to
	 * {@link SqrlJpaPersistenceFactory}
	 */
	@XmlElement(required = false)
	private String sqrlPersistenceFactoryClass = "com.github.sqrlserverjava.persistence.SqrlJpaPersistenceFactory";

	/**
	 * The cookie name to use for the SQRL correlator during authentication; defaults to {@code sqrlcorrelator}
	 */
	@XmlElement(required = false)
	private String correlatorCookieName = "sqrlcorrelator";

	/**
	 * The frequency with which to execute {@link SqrlPersistence#cleanUpExpiredEntries()} via {@link java.util.Timer};
	 * defaults to 15. If an alternate cleanup mechanism is in use (DB stored procedure, etc), this should be set to -1
	 * to disable the background task completely
	 */
	@XmlElement(required = false)
	private int cleanupTaskExecInMinutes = 15;

	/**
	 * The amount of time in millis to pause in between persistence queries to see if the SQRL client has finished
	 * authenticating users; defaults to 500
	 */
	@XmlElement(required = false)
	private long authSyncCheckInMillis = 500;

	/**
	 * The cookie name to use for the SQRL first nut during authentication; defaults to sqrlfirstnut
	 */
	@XmlElement(required = false)
	private String firstNutCookieName = "sqrlfirstnut";

	/**
	 * The domain to set on SQRL cookies. defaults to the domain (including subdomain) that the browser request came in
	 * on
	 */
	@XmlElement(required = false)
	private String cookieDomain = null;

	/**
	 * The path to set on SQRL cookies; defaults to "/"
	 */
	@XmlElement(required = false)
	private String cookiePath = "/";

	/**
	 * The path to the servlet or request handler that processes SQRL <b>client</b> (not web browser) requests. defaults
	 * to "/sqrllogin"
	 */
	@XmlElement(required = false)
	private String sqrlLoginServletPath = "/sqrllogin";

	/**
	 * Whether or not CPS is enabled for this server, defaults to true
	 */
	@XmlElement(required = false)
	private boolean enableCps = true;

	/**
	 * Computed from {@link #nutValidityInSeconds}, cannot be set directly
	 */
	@XmlTransient
	private long nutValidityInMillis = nutValidityInSeconds * 1000;

	public String[] getIpForwardedForHeaders() {
		return ipForwardedForHeaders;
	}

	public void setIpForwardedForHeaders(final String[] ipForwardedForHeaders) {
		this.ipForwardedForHeaders = ipForwardedForHeaders;
	}

	public int getNutValidityInSeconds() {
		return nutValidityInSeconds;
	}

	public long getNutValidityInMillis() {
		return nutValidityInMillis;
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
		this.nutValidityInMillis = nutValidityInSeconds * 1000;
	}

	public SqrlQrCodeImageFormat getQrCodeFileType() {
		return qrCodeFileType;
	}

	public void setQrCodeFileType(final SqrlQrCodeImageFormat qrCodeFileType) {
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

	public String getCookieDomain() {
		return cookieDomain;
	}

	public void setCookieDomain(final String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}

	public String getCookiePath() {
		return cookiePath;
	}

	public void setCookiePath(final String cookiePath) {
		this.cookiePath = cookiePath;
	}

	public long getAuthSyncCheckInMillis() {
		return authSyncCheckInMillis;
	}

	public void setAuthSyncCheckInMillis(final long authSyncCheckInMillis) {
		this.authSyncCheckInMillis = authSyncCheckInMillis;
	}

	public String getSqrlLoginServletPath() {
		return sqrlLoginServletPath;
	}

	public void setSqrlLoginServletPath(final String sqrlLoginServletPath) {
		this.sqrlLoginServletPath = sqrlLoginServletPath;
	}

	public boolean isEnableCps() {
		return enableCps;
	}

	public void setEnableCps(final boolean enableCps) {
		this.enableCps = enableCps;
	}
}
