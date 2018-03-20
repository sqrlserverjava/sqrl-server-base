package com.github.sqrlserverjava;

import static com.github.sqrlserverjava.enums.SqrlAuthenticationStatus.AUTHENTICATED_CPS;
import static com.github.sqrlserverjava.enums.SqrlInternalUserState.DISABLED;
import static com.github.sqrlserverjava.enums.SqrlInternalUserState.IDK_EXISTS;
import static com.github.sqrlserverjava.enums.SqrlInternalUserState.PIDK_EXISTS;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.backchannel.SqrlClientReply;
import com.github.sqrlserverjava.backchannel.SqrlClientRequest;
import com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil;
import com.github.sqrlserverjava.backchannel.SqrlClientRequestProcessor;
import com.github.sqrlserverjava.backchannel.SqrlNutToken;
import com.github.sqrlserverjava.backchannel.SqrlNutTokenUtil;
import com.github.sqrlserverjava.backchannel.SqrlTifResponse;
import com.github.sqrlserverjava.backchannel.SqrlTifResponse.SqrlTifResponseBuilder;
import com.github.sqrlserverjava.backchannel.SqrlTifFlag;
import com.github.sqrlserverjava.enums.SqrlAuthenticationStatus;
import com.github.sqrlserverjava.enums.SqrlInternalUserState;
import com.github.sqrlserverjava.enums.SqrlRequestCommand;
import com.github.sqrlserverjava.enums.SqrlRequestOpt;
import com.github.sqrlserverjava.enums.SqrlServerSideKey;
import com.github.sqrlserverjava.exception.SqrlClientRequestProcessingException;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.exception.SqrlInvalidRequestException;
import com.github.sqrlserverjava.persistence.SqrlAutoCloseablePersistence;
import com.github.sqrlserverjava.persistence.SqrlCorrelator;
import com.github.sqrlserverjava.util.SqrlConstants;
import com.github.sqrlserverjava.util.SqrlUtil;

/**
 * Core class for handling requests from SQRL clients
 * 
 * @author Dave Badia
 *
 */
public class SqrlClientFacingOperations {
	private static final Logger logger = LoggerFactory.getLogger(SqrlServerOperations.class);

	private final SqrlConfig config;
	private final SqrlConfigOperations configOperations;
	private final SqrlPersistenceFactory persistenceFactory;


	public SqrlClientFacingOperations(final SqrlConfig config, final SqrlConfigOperations configOperations) {
		if (config == null) {
			throw new IllegalArgumentException("SqrlConfig object must not be null", null);
		}
		this.config = config;
		this.configOperations = configOperations;
		this.persistenceFactory = configOperations.getSqrlPersistenceFactory();
	}

	private SqrlAutoCloseablePersistence createSqrlPersistence() {
		final SqrlPersistence sqrlPersistence = persistenceFactory.createSqrlPersistence();
		return new SqrlAutoCloseablePersistence(sqrlPersistence);
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
	public void handleSqrlClientRequest(final HttpServletRequest servletRequest,
			final HttpServletResponse servletResponse) throws IOException {
		SqrlClientRequestLoggingUtil.initLoggingHeader(servletRequest);
		if (logger.isInfoEnabled()) {
			logger.info(SqrlUtil.buildLogMessageForSqrlClientRequest(servletRequest).toString());
		}
		String correlator = "unknown";
		final SqrlTifResponseBuilder tifBuilder = new SqrlTifResponseBuilder();
		SqrlInternalUserState sqrlInternalUserState = SqrlInternalUserState.NONE_EXIST;
		String requestState = "invalid";
		try {
			String logHeader = "";
			SqrlClientRequest sqrlClientRequest = null;
			// Per the spec, SQRL transactions are atomic; so we create our persistence here and only commit after all
			// processing is completed successfully
			SqrlPersistence sqrlPersistence = createSqrlPersistence();
			Exception exception = null;
			try {
				// Get the correlator first. Then, if the request is invalid, we can update the auth page saying so
				correlator = SqrlClientRequest.parseCorrelatorOnly(servletRequest);

				sqrlClientRequest = new SqrlClientRequest(servletRequest, sqrlPersistence, configOperations);
				final SqrlClientRequestProcessor processor = new SqrlClientRequestProcessor(sqrlClientRequest,
						sqrlPersistence, config);

				logHeader = SqrlClientRequestLoggingUtil
						.updateLogHeader(new StringBuilder(sqrlClientRequest.getNegotiatedSqrlProtocolVersion())
								.append(" ").append(sqrlClientRequest.getClientCommand()).append(":: ").toString());

				Optional<String> mismatchDetail = testIfIpsMatch(sqrlClientRequest.getNut(), servletRequest);
				boolean ipsMatched = !mismatchDetail.isPresent();
				if (ipsMatched) {
					tifBuilder.addFlag(SqrlTifFlag.IPS_MATCHED);
				} else if (!sqrlClientRequest.getOptList().contains(SqrlRequestOpt.noiptest)) {
					throw new SqrlException(
							"Client did not sent noiptest opt and IPs did not match: " + mismatchDetail.get());
				}
				SqrlNutTokenUtil.validateNut(correlator, sqrlClientRequest.getNut(), config, sqrlPersistence);
				sqrlInternalUserState = processor.processClientCommand();
				if (sqrlInternalUserState == IDK_EXISTS) {
					tifBuilder.addFlag(SqrlTifFlag.CURRENT_ID_MATCH);
				} else if (sqrlInternalUserState == PIDK_EXISTS) {
					tifBuilder.addFlag(SqrlTifFlag.PREVIOUS_ID_MATCH);
				}
				servletResponse.setStatus(HttpServletResponse.SC_OK);
				requestState = "OK";
				sqrlPersistence.closeCommit();
			} catch (final SqrlException e) {
				exception = e;
				sqrlPersistence.closeRollback();
				tifBuilder.clearAllFlags().addFlag(SqrlTifFlag.COMMAND_FAILED);
				if (e instanceof SqrlClientRequestProcessingException) {
					tifBuilder.addFlag(((SqrlClientRequestProcessingException) e).getTifToAdd());
					logger.error("{}Received invalid SQRL request: {} of {}",
							SqrlClientRequestLoggingUtil.getLogHeader(), e.getMessage(),
							SqrlUtil.buildLogMessageForSqrlClientRequest(servletRequest), e);
				} else {
					logger.error("{}Generate exception processing SQRL request: {} of {}",
							SqrlClientRequestLoggingUtil.getLogHeader(), e.getMessage(),
							SqrlUtil.buildLogMessageForSqrlClientRequest(servletRequest), e);
				}
				// The SQRL spec is unclear about HTTP return codes. It mentions returning a 404 for an invalid request
				// but 404 is for page not found. We leave the use of 404 for an actual page not found condition and use
				// 500 here
				servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}

			// We have processed the request, success or failure. Now prep and transmit the reply
			String serverReplyString = ""; // for logging
			sqrlPersistence = persistenceFactory.createSqrlPersistence();
			try {
				final SqrlTifResponse tif = tifBuilder.createTif();
				final boolean isInErrorState = exception != null;
				final SqrlCorrelator sqrlCorrelator = sqrlPersistence.fetchSqrlCorrelatorRequired(correlator);
				serverReplyString = buildReply(servletRequest, sqrlClientRequest, tif, sqrlCorrelator,
						sqrlInternalUserState, isInErrorState);
				// Don't use AutoClosable here, we will handle it ourselves
				if (isInErrorState || sqrlInternalUserState == DISABLED) {
					tifBuilder.addFlag(SqrlTifFlag.COMMAND_FAILED);
					// update the correlator with the proper error state
					SqrlAuthenticationStatus authErrorState = SqrlAuthenticationStatus.ERROR_SQRL_INTERNAL;
					if (exception instanceof SqrlInvalidRequestException) {
						authErrorState = SqrlAuthenticationStatus.ERROR_BAD_REQUEST;
					} else if (sqrlInternalUserState == DISABLED) {
						authErrorState = SqrlAuthenticationStatus.SQRL_USER_DISABLED;
					}
					sqrlCorrelator.setAuthenticationStatus(authErrorState);
					// There should be no further requests so remove the parrot value
					if (sqrlCorrelator.getTransientAuthDataTable()
							.remove(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT) == null) {
						logger.warn("{}Tried to remove server parrot since we are in error state but it doesn't exist",
								SqrlClientRequestLoggingUtil.getLogHeader());
					}
				} else {
					// Store the serverReplyString in the server parrot value so we can validate it on the clients next
					// request
					sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_NAME_SERVER_PARROT,
							serverReplyString);
				}
				sqrlPersistence.closeCommit();
				transmitReplyToSqrlClient(servletResponse, serverReplyString);
				logger.info("{}Processed sqrl client request replied with tif 0x{}", logHeader, tif.toHexString());
			} catch (final SqrlException e) {
				sqrlPersistence.closeRollback();
				logger.error("{}Error sending SQRL reply with param: {}", logHeader, requestState,
						SqrlUtil.base64UrlDecodeToStringOrErrorMessage(serverReplyString), e);
				logger.debug("{}Request {}, responded with   B64: {}", logHeader, requestState, serverReplyString);
			}
		} finally {
			SqrlClientRequestLoggingUtil.clearLogHeader();
		}
	}

	private String buildReply(final HttpServletRequest servletRequest, final SqrlClientRequest sqrlRequest,
			final SqrlTifResponse tif, final SqrlCorrelator sqrlCorrelator, final SqrlInternalUserState sqrlInternalUserState,
			final boolean isInErrorState) throws SqrlException {
		final String logHeader = SqrlClientRequestLoggingUtil.getLogHeader();
		final SqrlPersistence sqrlPersistence = createSqrlPersistence();
		try {
			final URI sqrlServerUrl = new URI(servletRequest.getRequestURL().toString());
			final String subsequentRequestPath = configOperations.getSubsequentRequestPath(servletRequest);
			SqrlClientReply reply;
			if (isInErrorState) {
				// Send the error flag as nut and correlator, so if the client mistakenly sends a followup request it be
				// obvious to us
				reply = new SqrlClientReply(SqrlConstants.ERROR, tif, subsequentRequestPath, SqrlConstants.ERROR,
						Collections.emptyMap());
			} else {
				// Nut is one time use, so generate a new one for the reply
				final SqrlNutToken replyNut = SqrlNutTokenUtil.buildNut(config, configOperations, sqrlServerUrl,
						determineClientIpAddress(servletRequest, config));

				final Map<String, String> additionalDataTable = buildReplyAdditionalDataTable(sqrlRequest,
						sqrlCorrelator, sqrlInternalUserState, sqrlPersistence);
				// Build the final reply object
				reply = new SqrlClientReply(replyNut.asBase64UrlEncryptedNut(), tif, subsequentRequestPath,
						sqrlCorrelator.getCorrelatorString(), additionalDataTable);
			}

			final String serverReplyString = reply.toBase64();
			logger.debug("{}Build serverReplyString: {}", logHeader, serverReplyString);
			sqrlPersistence.closeCommit();
			return serverReplyString;
		} catch (final URISyntaxException e) {
			sqrlPersistence.closeRollback();
			throw new SqrlException(SqrlClientRequestLoggingUtil.getLogHeader()
					+ "Error converting servletRequest.getRequestURL() to URI.  " + "servletRequest.getRequestURL()="
					+ servletRequest.getRequestURL(), e);
		}
	}

	private Map<String, String> buildReplyAdditionalDataTable(final SqrlClientRequest sqrlRequest,
			final SqrlCorrelator sqrlCorrelator, final SqrlInternalUserState sqrlInternalUserState,
			final SqrlPersistence sqrlPersistence) throws SqrlException {
		// TreeMap to keep the items in order. Order is required as as the SQRL client will ignore everything
		// after an unrecognized option
		final Map<String, String> additionalDataTable = new TreeMap<>();

		// suk?
		if (shouldIncludeSukInReply(sqrlRequest, sqrlInternalUserState)) {
			if (sqrlPersistence.doesSqrlIdentityExistByIdk(sqrlRequest.getKey(SqrlServerSideKey.idk))) {
				final String sukString = SqrlRequestOpt.suk.toString();
				final String sukValue = sqrlPersistence
						.fetchSqrlIdentityDataItem(sqrlRequest.getKey(SqrlServerSideKey.idk), sukString);
				if (sukValue != null) {
					additionalDataTable.put(sukString, sukValue);
				}
			}
		}

		// cps?
		if (AUTHENTICATED_CPS == sqrlCorrelator.getAuthenticationStatus()) {
			// Generate and store our CPS nonce
			final String cpsNonce = UUID.randomUUID().toString();
			sqrlCorrelator.getTransientAuthDataTable().put(SqrlConstants.TRANSIENT_CPS_NONCE, cpsNonce);
			additionalDataTable.put("url", buildCpsLoginUrl(sqrlCorrelator, cpsNonce)); // NOT base64url encoded
		}

		return additionalDataTable;
	}

	private String buildCpsLoginUrl(final SqrlCorrelator sqrlCorrelator, final String cpsNonce) throws SqrlException {
		// The full sqrlAuth browser URL with the cps nonce as a param
		final String cpsLoginUrl = SqrlUtil.buildString(
				SqrlServerOperations.getBrowserFacingUrlAndContextPath().toString(),
				config.getSqrlLoginServletPath(), "?cor=", sqrlCorrelator.getCorrelatorString(), "&cps=", cpsNonce);
		try {
			new URL(cpsLoginUrl); // Sanity check
		} catch (final MalformedURLException e) {
			throw new SqrlException("Generated invalid CPS login URL of " + cpsLoginUrl, e);
		}
		return cpsLoginUrl;
	}

	private boolean shouldIncludeSukInReply(final SqrlClientRequest sqrlRequest,
			final SqrlInternalUserState sqrlInternalUserState) {
		if (sqrlRequest.getClientCommand() != SqrlRequestCommand.QUERY) {
			// suk is only returned during the query command
			return false;
		}
		return sqrlRequest.getOptList().contains(SqrlRequestOpt.suk)
				// https://www.grc.com/sqrl/semantics.htm says
				// The SQRL specification requires the SQRL server to automatically return the account's matching SUK
				// whenever it is able to anticipate that the client is likely to require it, such as when the server
				// contains a previous identity key, or when the account is disabled
				|| sqrlInternalUserState == DISABLED || sqrlInternalUserState == IDK_EXISTS
				|| sqrlInternalUserState == PIDK_EXISTS;
	}

	static InetAddress determineClientIpAddress(final HttpServletRequest servletRequest, final SqrlConfig config)
			throws SqrlException {
		final List<String> headersToCheckList = config.getIpForwardedForHeaderList();
		String ipToParse = null;
		for (final String headerToFind : headersToCheckList) {
			ipToParse = servletRequest.getHeader(headerToFind);
			if (SqrlUtil.isNotBlank(ipToParse)) {
				break;
			}
		}
		if (SqrlUtil.isBlank(ipToParse)) {
			ipToParse = servletRequest.getRemoteAddr();
		}
		try {
			return InetAddress.getByName(ipToParse);
		} catch (final UnknownHostException e) {
			throw new SqrlException("Caught exception trying to determine clients IP address", e);
		}
	}

	private void transmitReplyToSqrlClient(final HttpServletResponse response, final String serverReplyString)
			throws IOException {
		// Send the reply to the SQRL client
		response.setContentType("text/html;charset=utf-8");
		response.setContentLength(serverReplyString.length());
		try (PrintWriter writer = response.getWriter()) {
			writer.write(serverReplyString);
			writer.flush();
			writer.close();
		}
	}

	private Optional<String> testIfIpsMatch(final SqrlNutToken nut, final HttpServletRequest servletRequest)
			throws SqrlException {
		// TODO: check ipforwarded for headers
		// TODO: support IPV6
		final String ipAddressString = servletRequest.getRemoteAddr();
		if (SqrlUtil.isBlank(ipAddressString)) {
			throw new SqrlException(
					SqrlClientRequestLoggingUtil.getLogHeader() + "No ip address found in sqrl request");
		}
		final InetAddress requesterIpAddress = SqrlUtil.ipStringToInetAddresss(servletRequest.getRemoteAddr());
		return SqrlNutTokenUtil.validateInetAddress(requesterIpAddress, nut.getInetInt(), config);
	}
}
