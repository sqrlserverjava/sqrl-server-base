package com.github.dbadia.sqrl.server.backchannel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlAuthPageData;
import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.SqrlConfigOperations;
import com.github.dbadia.sqrl.server.SqrlException;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlPersistenceException;
import com.github.dbadia.sqrl.server.SqrlUtil;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif.TifBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * The core SQRL class which processes all SQRL requests and generates the appropriates responses
 * 
 * @author Dave Badia
 *
 */

public class SqrlServerOperations {
	private static final Logger logger = LoggerFactory.getLogger(SqrlServerOperations.class);

	static final long MAX_TIMESTAMP = Integer.toUnsignedLong(-1) * 1000L;

	private static final String COMMAND_QUERY = "query";
	private static final String COMMAND_IDENT = "ident";
	private static final String COMMAND_DISABLE = "disable";
	private static final String COMMAND_ENABLE = "enable";
	private static final String COMMAND_REMOVE = "remove";
	public static final String CORRELATOR_PARAM = "cor";

	private static final AtomicInteger COUNTER = new AtomicInteger(0);


	private final SqrlPersistence sqrlPersistence;
	private final SqrlConfigOperations sqrlConfigOperations;
	private final SqrlConfig config;

	/**
	 * Initializes the operations class with the given persistence and config
	 * 
	 * @param sqrlPersistence
	 *            the persistence to be used for storing and retreiving SQRL data
	 * @param config
	 *            the SQRL settings to be used
	 * @throws SqrlException
	 */
	public SqrlServerOperations(final SqrlPersistence sqrlPersistence, final SqrlConfig config) throws SqrlException {
		// SqrlPersistane
		if (sqrlPersistence == null) {
			throw new IllegalArgumentException("sqrlPersistence object must not be null", null);
		}
		this.sqrlPersistence = sqrlPersistence;
		if (config == null) {
			throw new IllegalArgumentException("SqrlConfig object must not be null", null);
		}
		this.sqrlConfigOperations = new SqrlConfigOperations(config);
		this.config = config;
	}

	/**
	 * Called to generate the data the server needs to display to allow a user to authenticate via SQRL
	 * 
	 * @param request
	 *            the servlet request
	 * @param userInetAddress
	 *            the IP address of the users browser
	 * @param qrCodeSizeInPixels
	 *            the size (in pixels) that the generated QR code will be
	 * @return the data the server needs to display to allow a user to authenticate via SQRL
	 * @throws SqrlException
	 *             if an error occurs
	 */
	public SqrlAuthPageData buildQrCodeForAuthPage(final HttpServletRequest request, final InetAddress userInetAddress,
			final int qrCodeSizeInPixels) throws SqrlException {
		final URI backchannelUri = sqrlConfigOperations.getBackchannelRequestUrl(request);
		final StringBuilder urlBuf = new StringBuilder(backchannelUri.toString());
		// Now we append the nut and our SFN
		// Even though urlBuf only contains the baseUrl, it's enough for NetUtil.inetAddressToInt
		final SqrlNutToken nut = buildNut(backchannelUri, userInetAddress);
		urlBuf.append("?nut=").append(nut.asSqBase64EncryptedNut());
		// Append the SFN
		String sfn = config.getServerFriendlyName();
		if (sfn == null) {
			// Auto compute the SFN from the server URL
			sfn = SqrlUtil.computeSfnFromUrl(request);
			config.setServerFriendlyName(sfn);
		}
		urlBuf.append("&sfn=").append(SqrlUtil.sqrlBase64UrlEncode(sfn));
		try {
			// Append our correlation id
			// Need correlation id to be unique to each Nut, so sha-256 the nut
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final String correlator = SqrlUtil
					.sqrlBase64UrlEncode(digest.digest(nut.asSqBase64EncryptedNut().getBytes()));
			urlBuf.append("&").append(CORRELATOR_PARAM).append("=").append(correlator);

			final String url = urlBuf.toString();
			final ByteArrayOutputStream qrBaos = generateQrCode(config, url, qrCodeSizeInPixels);
			return new SqrlAuthPageData(url, qrBaos, nut, correlator);
		} catch (final NoSuchAlgorithmException e) {
			throw new SqrlException(SqrlUtil.getLogHeader() + "Caught exception during correlator create", e);
		}
	}

	private SqrlNutToken buildNut(final URI backchannelUri, final InetAddress userInetAddress) throws SqrlException {
		final int inetInt = SqrlNutTokenUtil.inetAddressToInt(backchannelUri, userInetAddress);
		final int randomInt = config.getSecureRandom().nextInt();
		final long timestamp = System.currentTimeMillis();
		return new SqrlNutToken(inetInt, sqrlConfigOperations, COUNTER.getAndIncrement(), timestamp, randomInt);
	}

	/**
	 * The backchannel servlet which is accepting requests from SQRL clients should call this method to process the
	 * request
	 * 
	 * @param servletRequest
	 *            the servlet request
	 * @param servletResponse
	 *            the servlet response which will be populated accordingly
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public void handleSqrlClientRequest(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse)
			throws IOException {
		SqrlUtil.initLoggingHeader(servletRequest);
		if (logger.isInfoEnabled()) {
			logger.info("Processing SQRL client request: {}", SqrlUtil.buildRequestParamList(servletRequest));
		}
		String correlator = "unknown";
		try {
			final SqrlRequest request = new SqrlRequest(servletRequest, sqrlConfigOperations);
			correlator = request.extractFromServerString(CORRELATOR_PARAM);
			final String logHeader = SqrlUtil.updateLogHeader(new StringBuilder(correlator).append(" ")
					.append(request.getClientCommand()).append(":: ").toString());
			final TifBuilder tifBuilder = new TifBuilder(checkIfIpsMatch(request.getNut(), servletRequest));
			final SqrlNutToken nutToken = request.getNut();

			SqrlNutTokenUtil.validateNut(nutToken, config, sqrlPersistence);
			final boolean idkExistsInDataStore = processClientCommand(request, nutToken, request.getClientCommand(),
					tifBuilder, correlator);
			final String serverReplyString = buildReply(servletRequest, request, idkExistsInDataStore, tifBuilder,
					correlator);
			servletResponse.setStatus(HttpServletResponse.SC_OK);
			transmitReplyToSqrlClient(servletResponse, serverReplyString);
			logger.info("{}Request OK, responded with param: {}", logHeader,
					SqrlUtil.base64UrlDecodeToString(serverReplyString));
			logger.info("{}Request OK, responded with   B64: {}", logHeader, serverReplyString);
			return;
		} catch (final SqrlInvalidRequestException e) {
			logger.error(SqrlUtil.getLogHeader() + "Recevied invalid SQRL request: " + e.getMessage(), e);
		} catch (final SqrlException e) {
			logger.error(SqrlUtil.getLogHeader() + "General exception processing SQRL request: " + e.getMessage(), e);
		} finally {
			SqrlUtil.clearLogHeader();
		}
		// TODO: send sqrl error reply per spec
		servletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

	private boolean processClientCommand(final SqrlRequest request, final SqrlNutToken nutToken, final String command,
			final TifBuilder tifBuilder, final String correlator) throws SqrlException {
		final String logHeader = SqrlUtil.getLogHeader();
		logger.debug("{}Processing {} command for nut {}", logHeader, command, nutToken);
		final String idk = request.getIdk();
		boolean idkExistsInDataStore = sqrlPersistence.doesSqrlIdentityExistByIdk(idk);
		if (COMMAND_QUERY.equals(command)) {
			if (idkExistsInDataStore) {
				tifBuilder.setFlag(SqrlTif.TIF_CURRENT_ID_MATCH);
			} else if (request.hasPidk()) {
				final String result = sqrlPersistence.fetchSqrlIdentityDataItem(idk, request.getPidk());
				if (result == null) {
					sqrlPersistence.updateIdkForSqrlIdentity(request.getPidk(), idk);
					tifBuilder.setFlag(SqrlTif.TIF_PREVIOUS_ID_MATCH);
				}
			}
		} else if (COMMAND_IDENT.equals(command)) {
			final Map<String, String> keysToBeStored = request.getKeysToBeStored();
			sqrlPersistence.storeSqrlDataForSqrlIdentity(idk, keysToBeStored);
			idkExistsInDataStore = true;
			// Sanity check that the keys were actually stored
			for (final Map.Entry<String, String> entry : keysToBeStored.entrySet()) {
				final String value = sqrlPersistence.fetchSqrlIdentityDataItem(idk, entry.getKey());
				if (SqrlUtil.isBlank(value)) {
					throw new SqrlPersistenceException(
							SqrlUtil.getLogHeader() + "Stored value for " + entry.getKey() + " was null or empty");
				} else if (!entry.getValue().equals(value)) {
					throw new SqrlPersistenceException(
							SqrlUtil.getLogHeader() + "Stored value for " + entry.getKey() + " was corrupt");
				}
			}
			// Now that we know the data is stored, show the user as authenticated
			logger.info("{}User SQRL authenticated idk={}", logHeader, idk);
			sqrlPersistence.userAuthenticatedViaSqrl(idk, correlator);
		} else {
			// TODO: COMMAND_ENABLE and friends
			throw new SqrlException(SqrlUtil.getLogHeader() + "Unsupported client command " + command, null);
		}
		return idkExistsInDataStore;
	}

	private String buildReply(final HttpServletRequest servletRequest, final SqrlRequest sqrlRequest,
			final boolean idkExistsInDataStore, final TifBuilder tifBuilder, final String correlator)
					throws SqrlException {
		final String logHeader = SqrlUtil.getLogHeader();
		try {
			final URI sqrlServerUrl = new URI(servletRequest.getRequestURL().toString());

			// Nut is one time use, so generate a new one for the reply
			final SqrlNutToken replyNut = buildNut(sqrlServerUrl, determineClientIpAddress(servletRequest));

			// Add any additional data to the response based on the options the client requested
			// Keep additionalDataTable in order as SQRL client will ignore everything after an unrecognized option
			final Map<String, String> additionalDataTable = new TreeMap<>();
			for (final SqrlClientOpt clientOption : sqrlRequest.getOptList()) {
				switch (clientOption) {
				case suk:
				case vuk:
					if (idkExistsInDataStore) { // If the idk doesn't exist then we don't have these yet
						additionalDataTable.put(clientOption.toString(),
								sqrlPersistence.fetchSqrlIdentityDataItem(sqrlRequest.getIdk(), clientOption.toString()));
					}
					break;
				default:
					logger.error("{}Don't know how to reply to SQRL client opt {}", logHeader,
							clientOption);
					break;
				}
			}

			// Build the final reply object
			final String subsequentRequestPath = sqrlConfigOperations.getSubsequentRequestPath(servletRequest);

			final SqrlServerReply reply = new SqrlServerReply(replyNut.asSqBase64EncryptedNut(), tifBuilder.createTif(),
					subsequentRequestPath, correlator, additionalDataTable);
			final String serverReplyString = reply.toBase64();
			logger.debug("{}Build serverReplyString: {}", logHeader, serverReplyString);
			return serverReplyString;
		} catch (final URISyntaxException e) {
			throw new SqrlException(SqrlUtil.getLogHeader() + "Error converting servletRequest.getRequestURL() to URI.  "
					+ "servletRequest.getRequestURL()=" + servletRequest.getRequestURL(),
					e);
		}
	}

	private InetAddress determineClientIpAddress(final HttpServletRequest servletRequest) throws SqrlException {
		// TODO: header support
		try {
			return InetAddress.getByName(servletRequest.getRemoteAddr());
		} catch (final UnknownHostException e) {
			throw new SqrlException("Caught exception trying to determine clients IP address", e);
		} 
	}

	private void transmitReplyToSqrlClient(final HttpServletResponse response, final String serverReplyString)
			throws IOException, SqrlException {
		// Send the reply to the SQRL client
		response.setContentType("text/html;charset=utf-8");
		response.setContentLength(serverReplyString.length());
		try (PrintWriter writer = response.getWriter()) {
			writer.write(serverReplyString);
			writer.flush();
			writer.close();
		} catch (final IOException e) {
			throw e;
		}
	}

	private boolean checkIfIpsMatch(final SqrlNutToken nut, final HttpServletRequest servletRequest) throws SqrlException {
		final String ipAddressString = servletRequest.getRemoteAddr();
		if (SqrlUtil.isBlank(ipAddressString)) {
			throw new SqrlException(SqrlUtil.getLogHeader() + "No ip address found in sqrl request");
		}
		final InetAddress requesterIpAddress = SqrlUtil.ipStringToInetAddresss(servletRequest.getRemoteAddr());
		return SqrlNutTokenUtil.validateInetAddress(requesterIpAddress, nut.getInetInt());

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
			ImageIO.write(image, config.getQrCodeFileType().toString().toLowerCase(), os);
			return os;
		} catch (final IOException | WriterException e) {
			throw new SqrlException("Caught exception trying to determine clients IP address", e);
		}
	}
}
