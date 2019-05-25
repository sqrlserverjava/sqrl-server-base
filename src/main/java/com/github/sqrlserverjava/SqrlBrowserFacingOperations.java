package com.github.sqrlserverjava;

import static com.github.sqrlserverjava.enums.SqrlAuthenticationStatus.AUTHENTICATED_CPS;
import static com.github.sqrlserverjava.util.SqrlConstants.SCHEME_HTTPS_COLON;
import static com.github.sqrlserverjava.util.SqrlConstants.SCHEME_HTTP_COLON;
import static com.github.sqrlserverjava.util.SqrlUtil.buildString;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil;
import com.github.sqrlserverjava.backchannel.nut.SqrlNutToken;
import com.github.sqrlserverjava.backchannel.nut.SqrlNutTokenFactory;
import com.github.sqrlserverjava.enums.SqrlAuthenticationStatus;
import com.github.sqrlserverjava.enums.SqrlClientParam;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.persistence.SqrlAutoCloseablePersistence;
import com.github.sqrlserverjava.persistence.SqrlCorrelator;
import com.github.sqrlserverjava.util.SqrlConstants;
import com.github.sqrlserverjava.util.SqrlUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Core class for generating and handling requests a browser facing authentication page which uses SQRL
 * 
 * @author Dave Badia
 *
 */
public class SqrlBrowserFacingOperations {
	private static final Logger logger = LoggerFactory.getLogger(SqrlBrowserFacingOperations.class);

	private final SqrlConfig config;
	private final SqrlConfigOperations configOperations;


	public SqrlBrowserFacingOperations(final SqrlConfig config, final SqrlConfigOperations configOperations) {
		if (config == null) {
			throw new IllegalArgumentException("SqrlConfig object must not be null", null);
		}
		this.config = config;
		this.configOperations = configOperations;
	}

	/**
	 * Called to generate the data the server needs to display to allow a user to authenticate via SQRL
	 *
	 * @param servletRequest
	 *            the servlet request
	 * @param response
	 * @param qrCodeSizeInPixels
	 *            the size (in pixels) that the generated QR code will be
	 * @return the data the server needs to display to allow a user to authenticate via SQRL
	 * @throws SqrlException
	 *             if an error occurs
	 */
	public SqrlAuthPageData prepareSqrlAuthPageData(final HttpServletRequest servletRequest,
			final HttpServletResponse response, final int qrCodeSizeInPixels)
					throws SqrlException {
		final URI backchannelUri = configOperations.buildBackchannelRequestUrl(servletRequest);
		final StringBuilder urlBuf = new StringBuilder(backchannelUri.toString().length() + 100);
		urlBuf.append(backchannelUri.toString());
		final InetAddress userInetAddress = SqrlUtil.determineClientIpAddress(servletRequest, config);
		// Now we append the nut and our SFN
		final SqrlNutToken nut = SqrlNutTokenFactory.buildNut(config, configOperations, backchannelUri,
				userInetAddress);
		final String base64Nut = nut.asEncryptedBase64();
		urlBuf.append("?nut=").append(base64Nut);
		try (SqrlAutoCloseablePersistence sqrlPersistence = SqrlServerOperations
				.createSqrlPersistence(configOperations)) {
			// Append our correlation id
			// Need correlation id to be unique to each Nut, so sha-256 the nut
			final String correlator = UUID.randomUUID().toString();
			urlBuf.append("&").append(SqrlClientParam.cor.toString()).append("=").append(correlator);

			final String url = urlBuf.toString();
			final ByteArrayOutputStream qrBaos = generateQrCode(config, url, qrCodeSizeInPixels);
			// Store the url in the server parrot value so it will be there when the SQRL client makes the request
			final Date expiryTime = new Date(System.currentTimeMillis() + (1000 * config.getNutValidityInSeconds()));
			final SqrlCorrelator sqrlCorrelator = sqrlPersistence.createCorrelator(correlator, expiryTime);
			final Map<String, String> transientAuthDataTable = sqrlCorrelator.getTransientAuthDataTable();
			transientAuthDataTable.put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT, SqrlUtil.sqrlBase64UrlEncode(url));
			transientAuthDataTable.put(SqrlConstants.TRANSIENT_ENTRY_URL, buildEntryPointUrl(servletRequest));
			sqrlPersistence.closeCommit();
			final String cookieDomain = SqrlUtil.computeCookieDomain(servletRequest, config);

			// Correlator outlives the nut so extend the cookie expiry
			final int correlatorCookieAgeInSeconds = config.getNutValidityInSeconds() + 120;
			response.addCookie(SqrlUtil.createOrUpdateCookie(servletRequest, cookieDomain,
					config.getCorrelatorCookieName(), correlator, correlatorCookieAgeInSeconds, config));
			response.addCookie(SqrlUtil.createOrUpdateCookie(servletRequest, cookieDomain,
					config.getFirstNutCookieName(), base64Nut, config.getNutValidityInSeconds(), config)); // TODO: do
			// we need
			// this?
			return new SqrlAuthPageData(url, qrBaos, nut, correlator);
		}
	}

	private String buildEntryPointUrl(final HttpServletRequest request) throws SqrlException {
		try {
			final String originalEntryPointString = new URI(request.getRequestURL().toString())
					.resolve(request.getContextPath()).toURL().toString();
			// If we are being a reverse proxy, and the connection is clear between the proxy and the JEE server, it
			// will come through as http. Correct that to https here
			String entryPointString = originalEntryPointString.replace(SCHEME_HTTP_COLON, SCHEME_HTTPS_COLON);
			// the reverse proxy may introduce port 443 for SSL, remove it since it is redudant
			entryPointString = entryPointString.replace(":443", "");

			logger.debug("{}buildEntryPointUrl resulted in {} from {} ", SqrlClientRequestLoggingUtil.getLogHeader(),
					entryPointString, originalEntryPointString);
			return entryPointString;
		} catch (final URISyntaxException | MalformedURLException e) {
			throw new SqrlException(e, "Error computing currentRequestBrowserFacingUri");
		}
	}

	private ByteArrayOutputStream generateQrCode(final SqrlConfig config, final String urlToEmbed,
			final int qrCodeSizeInPixels) throws SqrlException {
		try {
			final Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
			hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");

			// Now with zxing version 3.2.1 you could change border size (white border size to just 1)
			hintMap.put(EncodeHintType.MARGIN, 1); /* default = 4 */
			hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

			final QRCodeWriter qrCodeWriter = new QRCodeWriter();
			final BitMatrix byteMatrix = qrCodeWriter.encode(urlToEmbed, BarcodeFormat.QR_CODE, qrCodeSizeInPixels,
					qrCodeSizeInPixels, hintMap);
			final int crunchifyWidth = byteMatrix.getWidth();
			final BufferedImage image = new BufferedImage(crunchifyWidth, crunchifyWidth, BufferedImage.TYPE_INT_RGB);
			image.createGraphics();

			final Graphics2D graphics = (Graphics2D) image.getGraphics();
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, crunchifyWidth, crunchifyWidth);
			graphics.setColor(Color.BLACK);

			for (int i = 0; i < crunchifyWidth; i++) {
				for (int j = 0; j < crunchifyWidth; j++) {
					if (byteMatrix.get(i, j)) {
						graphics.fillRect(i, j, 1, 1);
					}
				}
			}
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(image, config.getQrCodeImageFormat().toString().toLowerCase(), os);
			return os;
		} catch (final IOException | WriterException e) {
			throw new SqrlException(e, "Caught exception during QR code generation");
		}
	}

	/**
	 * Looks for the SQRL first nut cookie and extracts the time at which it expires
	 *
	 * @param request
	 *            the HTTP request
	 * @return the timestamp when the nut token expires
	 * @throws SqrlException
	 *             if an error occurs
	 */
	public long determineNutExpiry(final HttpServletRequest request) throws SqrlException {
		final String nutTokenString = SqrlUtil.findCookieValue(request, config.getFirstNutCookieName());
		if (nutTokenString == null) {
			throw new SqrlException(
					"firstNutCookie with name " + config.getFirstNutCookieName() + " was not found on http request");
		}
		final SqrlNutToken nut = SqrlNutTokenFactory.unmarshal(nutTokenString, configOperations);
		return nut.computeExpiresAt(config);
	}

	public void valdateCpsParamIfNecessary(final SqrlCorrelator sqrlCorrelator, final HttpServletRequest request)
			throws SqrlException {
		final String cpsParam = request.getParameter("cps");
		final SqrlAuthenticationStatus sqrlAuthStatus = sqrlCorrelator.getAuthenticationStatus();
		if (AUTHENTICATED_CPS == sqrlAuthStatus && SqrlUtil.isBlank(cpsParam)) {
			throw new SqrlException("server expected cps auth but got browser auth request instead");
		} else if (SqrlUtil.isNotBlank(cpsParam)) {
			final String persistenceCpsNonce = sqrlCorrelator.getTransientAuthDataTable()
					.get(SqrlConstants.TRANSIENT_CPS_NONCE);
			if (AUTHENTICATED_CPS != sqrlAuthStatus) {
				throw new SqrlException("cps param present but authstatus was " + sqrlAuthStatus);
			} else if (!cpsParam.equals(persistenceCpsNonce)) {
				throw new SqrlException(
						buildString("cps mismatch.  param=", cpsParam, " persistence=", persistenceCpsNonce));
			}
		}
	}

	/**
	 * Called by the web app once authentication is complete to cleanup any cookies set by the SQRL library
	 *
	 * @param request
	 *            the HTTP request
	 * @param response
	 *            the HTTP response
	 */
	public void deleteSqrlAuthCookies(final HttpServletRequest request, final HttpServletResponse response) {
		SqrlUtil.deleteCookies(request, response, config, config.getCorrelatorCookieName(),
				config.getFirstNutCookieName());
	}
}
