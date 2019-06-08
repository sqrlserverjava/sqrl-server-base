package com.github.sqrlserverjava;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.sqrlserverjava.backchannel.nut.SqrlNutToken0;
import com.github.sqrlserverjava.backchannel.nut.SqrlNutToken2Embedded;
import com.github.sqrlserverjava.backchannel.nut.SqrlNutTokenLegacyFormat;
import com.github.sqrlserverjava.backchannel.nut.SqrlNutToken1SingleBlockFormat;
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
public class SqrlConfig {
	// @formatter:off
	/* *********************************************************************************************/
	/* *************************************** REQUIRED ********************************************/
	/* *********************************************************************************************/
	/**
	 * REQUIRED: The path (full URL or partial URI) of the backchannel servlet which the SQRL clients will call.  
	 * For example:  https://sqrljava.com:20000/sqrlexample/sqrlbc or /sqrlexample/sqrlbc or /sqrlbc
	 */
	private String backchannelServletPath;

	/**
	 * REQUIRED: The base64 encoded 16 byte AES key used to encrypt {@link SqrlNutToken0}
	 */
	private String aesKeyBase64;

	/* *********************************************************************************************/
	/* *************************************** RECOMMENDED *****************************************/
	/* *********************************************************************************************/
	/**
	 * The header name (X-Forwarded-For, etc) from which to get the users real IP. SQRL
	 * requires the users real IP to respond to the client correctly.  Multiple header names
	 * can be defined; the headers will be checked in the order given.
	 * 
	 * Default: X-Forwarded-For
	 */
	private String[] ipForwardedForHeader;

	/* *********************************************************************************************/
	/* *************************************** OPTIONAL ********************************************/
	/* *********************************************************************************************/

	/**
	 * The full classname of the SqrlClientAuthStateUpdater that will push status updates to the client
	 * browser. If this value is not defined and the sqrl-sever-atomosphere library is on the classpath, this
	 * will automatically be set to com.github.sqrlserverjava.atmosphere.AtmosphereClientAuthStateUpdater
	 * 
	 * Default: com.github.sqrlserverjava.atmosphere.AtmosphereClientAuthStateUpdater (if the sqrl-sever-atomosphere 
	 * library is present)  
	 * 
	 * @see {@link SqrlClientAuthStateUpdater}
	 * @see <a href="https://github.com/sqrlserverjava/sqrl-server-atmosphere">sqrl-server-atmosphere</a>
	 */
	private String clientAuthStateUpdaterClass = null;

	/**
	 * The amount of time the SQRL "Nut" will be valid for; default is 15 minutes. That is the maximum amount of time
	 * that can pass between us (server) generating the QR code and us receiving the clients response
	 *
	 * It is strongly recommended that this value be set to 15 minutes (default) or more as this is a new protocol. The
	 * user may use SQRl quite infrequently at first and my need time to recall their SQRL password, remember how it
	 * works etc.
	 * 
	 * Default: 900
	 */
	private int nutValidityInSeconds = (int) TimeUnit.MINUTES.toSeconds(15);

	/**
	 * Computed from {@link #nutValidityInSeconds}, cannot be set directly.
	 * Updated by {@link #setNutValidityInSeconds(int)} when invoked
	 */
	private long nutValidityInMillis = nutValidityInSeconds * 1000;

	/**
	 * The image format to generate QR codes in
	 * 
	 * Default: PNG
	 */
	private SqrlQrCodeImageFormat qrCodeImageFormat = SqrlQrCodeImageFormat.PNG;

	/**
	 * The secureRandom instance that is used to generate various random tokens; defaults to
	 * {@link SecureRandom#SecureRandom()}. Can only be set via setter, not by config file
	 * 
	 * @see #setSecureRandom(SecureRandom)
	 */
	private SecureRandom secureRandom;

	/**
	 * The SQRL persistence provider class which implements {@link SqrlPersistenceFactory}
	 * 
	 * Default: {@link SqrlJpaPersistenceFactory}
	 */
	private String sqrlPersistenceFactoryClass = "com.github.sqrlserverjava.persistence.SqrlJpaPersistenceFactory";

	/**
	 * The frequency with which to execute {@link SqrlPersistence#cleanUpExpiredEntries()} via {@link java.util.Timer};
	 * Default: 15. 
	 * 
	 * If an alternate cleanup mechanism is in use (DB stored procedure, etc), this should be set to -1
	 * to disable the background task completely
	 */
	private int cleanupTaskExecInMinutes = 15;

	/**
	 * The amount of time in millis to pause in between persistence queries to see if the SQRL client has finished
	 * authenticating users.
	 * 
	 * Default: 500
	 */
	private long authSyncCheckInMillis = 500;

	/**
	 * The path to the servlet or request handler that processes SQRL <b>client</b> (not web browser) requests.
	 * Default: /sqrllogin
	 */
	private String sqrlLoginServletPath = "/sqrllogin"; // TODO: what is the difference between this and sqrlbackchannel?

	/**
	 * Whether or not CPS is enabled for this server
	 * Default: true
	 */
	private boolean enableCps = true;

	/**
	 * When enableCps=true, this is the URI where the user will be redirected to if the CPS authentication
	 * is cancelled or fails.  Ignored when enableCps=false
	 * Default: n/a
	 */
	private String cpsCancelUri;

	/**
	 * The cookie name to use for the SQRL correlator during authentication
	 * Default: sqrlcorrelator
	 */
	private String correlatorCookieName = "sqrlcorrelator";
	/**
	 * The cookie name to use for storage of first SQRL nut generated during authentication.
	 * 
	 * Default: sqrlfirstnut
	 */
	private String firstNutCookieName = "sqrlfirstnut";

	/**
	 * The domain to set on all SQRL cookies. 
	 * 
	 * Default: the domain (including subdomain) that the browser request came in on
	 */
	private String cookieDomain = null;

	/**
	 * The path to set on SQRL cookies
	 * Default: /
	 */
	private String cookiePath = "/";

	/**
	 * The format ID to use when generating the SQRL nut token<br/> 
	 * Default: 2
	 * 
	 * @see SqrlNutToken2Embedded
	 * @see SqrlNutTokenLegacyFormat
	 * @see SqrlNutToken1SingleBlockFormat
	 */
	private int	sqrlNutTokenFormat = 2;

	/**
	 * By default, the SQRL client generates the identity based on the domain of the website.  
	 * This setting can be used to tell the client to include one or more URIs after the domain in the computation.  
	 * Default: 0 (domain only)
	 * 
	 */
	private int	sqrlDomainExtensionSpecifierUriCount = 0;

	// @formatter:on

	/* *********************************************************************************************/
	/* *************************************** ACCESSORS *******************************************/
	/* *********************************************************************************************/

	@XmlElement(required = true)
	public String getBackchannelServletPath() {
		return backchannelServletPath;
	}

	// @formatter:off
	/**
	 * Required: sets the path to the servlet endpoint which will handle SQRL client requests, can be either a full URL,
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

	@XmlElement(required = true)
	public String getAesKeyBase64() {
		return aesKeyBase64;
	}

	/**
	 * @see #aesKeyBytes
	 */
	public void setAesKeyBase64(final String aesKeyBase64) {
		this.aesKeyBase64 = aesKeyBase64;
	}

	/* *********************************************************************************************/
	/* *************************************** OPTIONAL ********************************************/
	/* *********************************************************************************************/


	@XmlElement(required = false)
	public String getClientAuthStateUpdaterClass() {
		return clientAuthStateUpdaterClass;
	}

	/**
	 * @see #clientAuthStateUpdaterClass
	 */
	public void setClientAuthStateUpdaterClass(final String clientAuthStateUpdaterClass) {
		this.clientAuthStateUpdaterClass = clientAuthStateUpdaterClass;
	}

	@XmlElement(required = false)
	public int getNutValidityInSeconds() {
		return nutValidityInSeconds;
	}

	/**
	 * 
	 * @see nutValidityInSeconds
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

	/**
	 * Helper method which converts to millis
	 */
	public long getNutValidityInMillis() {
		return nutValidityInMillis;
	}

	@XmlElement(required = false)
	public SqrlQrCodeImageFormat getQrCodeImageFormat() {
		return qrCodeImageFormat;
	}

	/**
	 * @see #qrCodeImageFormat
	 */
	public void setQrCodeImageFormat(final SqrlQrCodeImageFormat qrCodeFileType) {
		this.qrCodeImageFormat = qrCodeFileType;
	}	

	@XmlElement(required = false)
	public String[] getIpForwardedForHeader() {
		return ipForwardedForHeader;
	}

	/**
	 * @see #ipForwardedForHeaders
	 */
	public void setIpForwardedForHeader(final String[] ipForwardedForHeader) {
		this.ipForwardedForHeader = ipForwardedForHeader;
	}

	public List<String> getIpForwardedForHeaderList() {
		if(ipForwardedForHeader == null) {
			return Collections.singletonList("X-Forwarded-For");
		}
		return Arrays.asList(ipForwardedForHeader);
	}

	public String getSqrlPersistenceFactoryClass() {
		return sqrlPersistenceFactoryClass;
	}

	/**
	 * @see #sqrlPersistenceFactoryClass
	 */
	public void setSqrlPersistenceFactoryClass(final String sqrlPersistenceFactoryClass) {
		this.sqrlPersistenceFactoryClass = sqrlPersistenceFactoryClass;
	}

	@XmlElement(required = false)
	public int getCleanupTaskExecInMinutes() {
		return cleanupTaskExecInMinutes;
	}

	/**
	 * @see #sqrlPersistenceFactoryClass
	 */
	public void setCleanupTaskExecInMinutes(final int cleanupTaskExecInMinutes) {
		this.cleanupTaskExecInMinutes = cleanupTaskExecInMinutes;
	}

	@XmlElement(required = false)
	public long getAuthSyncCheckInMillis() {
		return authSyncCheckInMillis;
	}

	/**
	 * @see #authSyncCheckInMillis
	 */
	public void setAuthSyncCheckInMillis(final long authSyncCheckInMillis) {
		this.authSyncCheckInMillis = authSyncCheckInMillis;
	}

	@XmlElement(required = false)
	public String getSqrlLoginServletPath() {
		return sqrlLoginServletPath;
	}

	/**
	 * @see #sqrlLoginServletPath
	 */
	public void setSqrlLoginServletPath(final String sqrlLoginServletPath) {
		this.sqrlLoginServletPath = sqrlLoginServletPath;
	}

	@XmlElement(required = false)
	public boolean isEnableCps() {
		return enableCps;
	}

	/**
	 * @see #enableCps
	 */
	public void setEnableCps(final boolean enableCps) {
		this.enableCps = enableCps;
	}

	@XmlElement(required = false)
	public String getCorrelatorCookieName() {
		return correlatorCookieName;
	}

	/**
	 * @see #correlatorCookieName
	 */
	public void setCorrelatorCookieName(final String correlatorCookieName) {
		this.correlatorCookieName = correlatorCookieName;
	}

	@XmlElement(required = false)
	public String getFirstNutCookieName() {
		return firstNutCookieName;
	}

	/**
	 * @see #firstNutCookieName
	 */
	public void setFirstNutCookieName(final String firstNutCookieName) {
		this.firstNutCookieName = firstNutCookieName;
	}

	@XmlElement(required = false)
	public String getCookieDomain() {
		return cookieDomain;
	}

	/**
	 * @see #cookieDomain
	 */
	public void setCookieDomain(final String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}

	@XmlElement(required = false)
	public String getCookiePath() {
		return cookiePath;
	}

	/**
	 * @see #cookiePath
	 */
	public void setCookiePath(final String cookiePath) {
		this.cookiePath = cookiePath;
	}

	@XmlTransient // Can only be overridden in code
	public SecureRandom getSecureRandom() {
		return secureRandom;
	}

	/**
	 * @see #secureRandom
	 */
	public void setSecureRandom(final SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
	}

	/**
	 * The system time is certainly not part of our config, but this
	 * provides an easy way for test cases to hardcode a time to 
	 * generate repeatable tests
	 * 
	 * @return {@link System#currentTimeMillis()} 
	 */
	public long getCurrentTimeMs() {
		return System.currentTimeMillis();
	}

	@XmlElement(required = false)
	public int getSqrlNutTokenFormat() {
		return sqrlNutTokenFormat;
	}

	/**
	 * @see #sqrlNutTokenFormat
	 */
	public void setSqrlNutTokenFormat(final int sqrlNutTokenFormat) {
		this.sqrlNutTokenFormat = sqrlNutTokenFormat;
	}

	/**
	 * @return the cpsCancelUri
	 */
	public String getCpsCancelUri() {
		return cpsCancelUri;
	}

	/**
	 * @param cpsCancelUri
	 *            the cpsCancelUri to set
	 */
	public void setCpsCancelUri(final String cpsCancelUri) {
		this.cpsCancelUri = cpsCancelUri;
	}

	/**
	 * @return the sqrlDomainExtensionSpecifierUriCount
	 */
	public int getSqrlDomainExtensionSpecifierUriCount() {
		return sqrlDomainExtensionSpecifierUriCount;
	}

	/**
	 * @param sqrlDomainExtensionSpecifierUriCount
	 *            the sqrlDomainExtensionSpecifierUriCount to set
	 */
	public void setSqrlDomainExtensionSpecifierUriCount(final int sqrlDomainExtensionSpecifierUriCount) {
		this.sqrlDomainExtensionSpecifierUriCount = sqrlDomainExtensionSpecifierUriCount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aesKeyBase64 == null) ? 0 : aesKeyBase64.hashCode());
		result = prime * result + (int) (authSyncCheckInMillis ^ (authSyncCheckInMillis >>> 32));
		result = prime * result + ((backchannelServletPath == null) ? 0 : backchannelServletPath.hashCode());
		result = prime * result + cleanupTaskExecInMinutes;
		result = prime * result + ((clientAuthStateUpdaterClass == null) ? 0 : clientAuthStateUpdaterClass.hashCode());
		result = prime * result + ((cookieDomain == null) ? 0 : cookieDomain.hashCode());
		result = prime * result + ((cookiePath == null) ? 0 : cookiePath.hashCode());
		result = prime * result + ((correlatorCookieName == null) ? 0 : correlatorCookieName.hashCode());
		result = prime * result + ((cpsCancelUri == null) ? 0 : cpsCancelUri.hashCode());
		result = prime * result + (enableCps ? 1231 : 1237);
		result = prime * result + ((firstNutCookieName == null) ? 0 : firstNutCookieName.hashCode());
		result = prime * result + Arrays.hashCode(ipForwardedForHeader);
		result = prime * result + (int) (nutValidityInMillis ^ (nutValidityInMillis >>> 32));
		result = prime * result + nutValidityInSeconds;
		result = prime * result + ((qrCodeImageFormat == null) ? 0 : qrCodeImageFormat.hashCode());
		result = prime * result + sqrlDomainExtensionSpecifierUriCount;
		result = prime * result + ((sqrlLoginServletPath == null) ? 0 : sqrlLoginServletPath.hashCode());
		result = prime * result + sqrlNutTokenFormat;
		result = prime * result + ((sqrlPersistenceFactoryClass == null) ? 0 : sqrlPersistenceFactoryClass.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SqrlConfig other = (SqrlConfig) obj;
		if (aesKeyBase64 == null) {
			if (other.aesKeyBase64 != null) {
				return false;
			}
		} else if (!aesKeyBase64.equals(other.aesKeyBase64)) {
			return false;
		}
		if (authSyncCheckInMillis != other.authSyncCheckInMillis) {
			return false;
		}
		if (backchannelServletPath == null) {
			if (other.backchannelServletPath != null) {
				return false;
			}
		} else if (!backchannelServletPath.equals(other.backchannelServletPath)) {
			return false;
		}
		if (cleanupTaskExecInMinutes != other.cleanupTaskExecInMinutes) {
			return false;
		}
		if (clientAuthStateUpdaterClass == null) {
			if (other.clientAuthStateUpdaterClass != null) {
				return false;
			}
		} else if (!clientAuthStateUpdaterClass.equals(other.clientAuthStateUpdaterClass)) {
			return false;
		}
		if (cookieDomain == null) {
			if (other.cookieDomain != null) {
				return false;
			}
		} else if (!cookieDomain.equals(other.cookieDomain)) {
			return false;
		}
		if (cookiePath == null) {
			if (other.cookiePath != null) {
				return false;
			}
		} else if (!cookiePath.equals(other.cookiePath)) {
			return false;
		}
		if (correlatorCookieName == null) {
			if (other.correlatorCookieName != null) {
				return false;
			}
		} else if (!correlatorCookieName.equals(other.correlatorCookieName)) {
			return false;
		}
		if (cpsCancelUri == null) {
			if (other.cpsCancelUri != null) {
				return false;
			}
		} else if (!cpsCancelUri.equals(other.cpsCancelUri)) {
			return false;
		}
		if (enableCps != other.enableCps) {
			return false;
		}
		if (firstNutCookieName == null) {
			if (other.firstNutCookieName != null) {
				return false;
			}
		} else if (!firstNutCookieName.equals(other.firstNutCookieName)) {
			return false;
		}
		if (!Arrays.equals(ipForwardedForHeader, other.ipForwardedForHeader)) {
			return false;
		}
		if (nutValidityInMillis != other.nutValidityInMillis) {
			return false;
		}
		if (nutValidityInSeconds != other.nutValidityInSeconds) {
			return false;
		}
		if (qrCodeImageFormat != other.qrCodeImageFormat) {
			return false;
		}
		if (sqrlDomainExtensionSpecifierUriCount != other.sqrlDomainExtensionSpecifierUriCount) {
			return false;
		}
		if (sqrlLoginServletPath == null) {
			if (other.sqrlLoginServletPath != null) {
				return false;
			}
		} else if (!sqrlLoginServletPath.equals(other.sqrlLoginServletPath)) {
			return false;
		}
		if (sqrlNutTokenFormat != other.sqrlNutTokenFormat) {
			return false;
		}
		if (sqrlPersistenceFactoryClass == null) {
			if (other.sqrlPersistenceFactoryClass != null) {
				return false;
			}
		} else if (!sqrlPersistenceFactoryClass.equals(other.sqrlPersistenceFactoryClass)) {
			return false;
		}
		return true;
	}
}
