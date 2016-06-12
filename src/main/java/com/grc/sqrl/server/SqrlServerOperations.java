package com.grc.sqrl.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grc.sqrl.server.SqrlConfig.FileType;
import com.grc.sqrl.server.backchannel.SqrlClientOpt;
import com.grc.sqrl.server.backchannel.SqrlInvalidRequestException;
import com.grc.sqrl.server.backchannel.SqrlNutToken;
import com.grc.sqrl.server.backchannel.SqrlNutTokenUtil;
import com.grc.sqrl.server.backchannel.SqrlRequest;
import com.grc.sqrl.server.backchannel.SqrlServerReply;
import com.grc.sqrl.server.backchannel.SqrlTif;
import com.grc.sqrl.server.backchannel.SqrlTif.TifBuilder;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;

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


	private final SqrlIdentityPersistance sqrlPersistance;
	private final SqrlConfigOperations sqrlConfigOperations;
	private final SqrlConfig config;

	public SqrlServerOperations(final SqrlIdentityPersistance sqrlPersistance, final SqrlConfig config) throws SqrlException {
		// SqrlPersistane
		if (sqrlPersistance == null) {
			throw new IllegalArgumentException("SqrlPersistance object must not be null", null);
		}
		this.sqrlPersistance = sqrlPersistance;
		if (config == null) {
			throw new IllegalArgumentException("SqrlConfig object must not be null", null);
		}
		this.sqrlConfigOperations = new SqrlConfigOperations(config);
		this.config = config;
	}

	/**
	 * Called to generate the data the server needs to display to allow a user to authenticate via SQRL
	 * 
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
			final SqrlAuthPageData authPageData = new SqrlAuthPageData(url, qrBaos, nut, correlator);
			return authPageData;
		} catch (final NoSuchAlgorithmException e) {
			throw new SqrlException(SqrlUtil.getLogHeader() + "Caught exception during correlator create", e);
		}
	}

	private SqrlNutToken buildNut(final URI backchannelUri, final InetAddress userInetAddress) throws SqrlException {
		final int inetInt = SqrlNutTokenUtil.inetAddressToInt(backchannelUri, userInetAddress);
		final int randomInt = config.getSecureRandom().nextInt();
		final long timestamp = System.currentTimeMillis();
		final SqrlNutToken nut = new SqrlNutToken(inetInt, sqrlConfigOperations, COUNTER.getAndIncrement(), timestamp, randomInt);
		return nut;
	}

	public void handleSqrlClientRequest(final HttpServletRequest servletRequest, final HttpServletResponse response)
			throws IOException {
		SqrlUtil.initLoggingHeader(servletRequest);
		if (logger.isInfoEnabled()) {
			logger.info("Processing SQRL client request: {}", buildRequestParamList(servletRequest));
		}
		String correlator = "unknown";
		try {
			final SqrlRequest request = new SqrlRequest(servletRequest, sqrlConfigOperations);
			correlator = request.extractFromServerString(CORRELATOR_PARAM);
			final String logHeader = SqrlUtil.updateLogHeader(new StringBuilder(correlator).append(" ")
					.append(request.getClientCommand()).append(":: ").toString());
			final TifBuilder tifBuilder = new TifBuilder(checkIfIpsMatch(request.getNut(), servletRequest));
			final SqrlNutToken nutToken = request.getNut();

			SqrlNutTokenUtil.validateNut(nutToken, config, sqrlPersistance);
			final boolean idkExistsInDataStore = processClientCommand(request, nutToken, request.getClientCommand(),
					tifBuilder, correlator);
			final String serverReplyString = buildReply(servletRequest, request, idkExistsInDataStore, tifBuilder,
					correlator);
			transmitReplyToSqrlClient(response, serverReplyString);
			logger.info("{}Request OK, responded with param: {}", logHeader,
					SqrlUtil.base64UrlDecodeToString(serverReplyString));
			logger.debug("{}Request OK, responded with   B64: {}", logHeader, serverReplyString);
			return;
		} catch (final SqrlInvalidRequestException e) {
			logger.error(SqrlUtil.getLogHeader() + "Recevied invalid SQRL request: " + e.getMessage(), e);
		} catch (final SqrlException e) {
			logger.error(SqrlUtil.getLogHeader() + "General exception processing SQRL request: " + e.getMessage(), e);
		} finally {
			SqrlUtil.clearLogHeader();
		}
		// TODO: send sqrl error reply per spec
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

	private boolean processClientCommand(final SqrlRequest request, final SqrlNutToken nutToken, final String command,
			final TifBuilder tifBuilder, final String correlator) throws SqrlException {
		final String logHeader = SqrlUtil.getLogHeader();
		logger.debug("{}Processing {} command for nut {}", logHeader, command, nutToken);
		final String idk = request.getIdk();
		boolean idkExistsInDataStore = sqrlPersistance.doesSqrlIdentityExistByIdk(idk);
		if (COMMAND_QUERY.equals(command)) {
			if (idkExistsInDataStore) {
				tifBuilder.setFlag(SqrlTif.TIF_CURRENT_ID_MATCH);
			} else if (request.hasPidk() && sqrlPersistance.doesSqrlIdentityExistByIdk(request.getPidk())) {
				sqrlPersistance.updateIdkForSqrlIdentity(request.getPidk(), idk);
				tifBuilder.setFlag(SqrlTif.TIF_PREVIOUS_ID_MATCH);
			}
		} else if (COMMAND_IDENT.equals(command)) {
			final Map<String, String> keysToBeStored = request.getKeysToBeStored();
			sqrlPersistance.storeSqrlDataForSqrlIdentity(idk, keysToBeStored);
			idkExistsInDataStore = true;
			// Sanity check that the keys were actually stored
			for (final Map.Entry<String, String> entry : keysToBeStored.entrySet()) {
				final String value = sqrlPersistance.fetchSqrlIdentityDataItem(idk, entry.getKey());
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
			sqrlPersistance.userAuthenticatedViaSqrl(idk, correlator);
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
								sqrlPersistance.fetchSqrlIdentityDataItem(sqrlRequest.getIdk(), clientOption.toString()));
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
		// TODO: header
		// support
		try {
			final InetAddress clientAddress = InetAddress.getByName(servletRequest.getRemoteAddr());
			return clientAddress;
		} catch (final UnknownHostException e) {
			throw new SqrlException("Caught exception trying to determine clients IP address", e);
		} 
	}

	private void transmitReplyToSqrlClient(final HttpServletResponse response, final String serverReplyString)
			throws IOException, SqrlException {
		final String logHeader = SqrlUtil.getLogHeader();
		// Send the reply to the SQRL client
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
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

	private ByteArrayOutputStream generateQrCode(final SqrlConfig config, final String url, final int qrCodeSizeInPixels)
			throws SqrlException {
		ImageType imageType = null;
		if (config.getQrCodeFileType() == FileType.PNG) {
			imageType = ImageType.PNG;
		} else if (config.getQrCodeFileType() == FileType.GIF) {
			imageType = ImageType.GIF;
		}
		return QRCode.from(url).to(imageType).withSize(qrCodeSizeInPixels, qrCodeSizeInPixels).stream();
	}

	public static String buildRequestParamList(final HttpServletRequest servletRequest) {
		final Enumeration<String> params = servletRequest.getParameterNames();
		final StringBuilder buf = new StringBuilder();
		while (params.hasMoreElements()) {
			final String paramName = params.nextElement();
			buf.append(paramName).append("=").append(servletRequest.getParameter(paramName)).append("  ");
		}
		return buf.toString();
	}

}
