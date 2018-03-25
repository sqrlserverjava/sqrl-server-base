package com.github.sqrlserverjava.backchannel;

import static com.github.sqrlserverjava.enums.SqrlInternalUserState.IDK_EXISTS;
import static com.github.sqrlserverjava.enums.SqrlInternalUserState.NONE_EXIST;
import static com.github.sqrlserverjava.enums.SqrlInternalUserState.PIDK_EXISTS;
import static com.github.sqrlserverjava.enums.SqrlServerSideKey.idk;
import static com.github.sqrlserverjava.enums.SqrlServerSideKey.pidk;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlPersistence;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.enums.SqrlAuthenticationStatus;
import com.github.sqrlserverjava.enums.SqrlIdentityFlag;
import com.github.sqrlserverjava.enums.SqrlInternalUserState;
import com.github.sqrlserverjava.enums.SqrlRequestCommand;
import com.github.sqrlserverjava.enums.SqrlRequestOpt;
import com.github.sqrlserverjava.exception.SqrlClientRequestProcessingException;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.exception.SqrlInvalidRequestException;
import com.github.sqrlserverjava.persistence.SqrlCorrelator;

public class SqrlClientRequestProcessor {
	private static final Logger logger = LoggerFactory.getLogger(SqrlServerOperations.class);

	private final SqrlClientRequest		sqrlClientRequest;
	private final String				sqrlIdk;
	private final SqrlRequestCommand	command;
	private final String				logHeader;
	private final String				correlator;
	private final SqrlPersistence		sqrlPersistence;
	private final SqrlConfig			sqrlconfig;

	private SqrlInternalUserState		sqrlInternalUserState	= NONE_EXIST;

	public SqrlClientRequestProcessor(final SqrlClientRequest sqrlClientRequest,
			final SqrlPersistence sqrlPersistence, final SqrlConfig sqrlConfig) throws SqrlInvalidRequestException {
		super();
		// Cache the logHeader since we use it a lot and it won't change here
		this.logHeader = SqrlClientRequestLoggingUtil.getLogHeader();
		this.sqrlPersistence = sqrlPersistence;
		this.sqrlClientRequest = sqrlClientRequest;
		this.sqrlconfig = sqrlConfig;
		this.sqrlIdk = sqrlClientRequest.getKey(idk);
		this.command = sqrlClientRequest.getClientCommand();
		this.correlator = sqrlClientRequest.getCorrelator();
	}

	/**
	 * Processes the request sent by a SQRL client
	 *
	 * @return true if the idk exists in persistence upon return
	 */
	public SqrlInternalUserState processClientCommand() throws SqrlException {
		sqrlInternalUserState = NONE_EXIST;

		final boolean idkExistsInPersistence = sqrlPersistence.doesSqrlIdentityExistByIdk(sqrlIdk);
		// Set IDK /PIDK Tifs
		if (idkExistsInPersistence) {
			sqrlInternalUserState = IDK_EXISTS;
		} else if (sqrlClientRequest.hasKey(pidk)
				&& sqrlPersistence.doesSqrlIdentityExistByIdk(sqrlClientRequest.getKey(pidk))) {
			sqrlInternalUserState = PIDK_EXISTS;
		}

		processCommand();
		if (command.shouldProcessOpts()) {
			processNonKeyOptions();
		}
		return sqrlInternalUserState;
	}

	private void updateOptValueAsNeeded(final SqrlIdentityFlag flag, final SqrlRequestOpt opt) {
		if (opt != null) {
			final boolean clientValue = sqrlClientRequest.getOptList().contains(opt);
			final boolean dbValue = sqrlPersistence.fetchSqrlFlagForIdentity(sqrlIdk, flag);
			if (clientValue != dbValue) { // update it
				logger.debug("{}Updating SQRL flag {} from {} to {}", logHeader, opt, dbValue,
						clientValue);
				sqrlPersistence.setSqrlFlagForIdentity(sqrlIdk, flag, clientValue);
				// TODO_AUDIT, client updated value to clientSet
			}
		}
	}

	/**
	 * Processes all SQRL opt items, except those which request previously stored keys (suk, vuk, etc)
	 */
	private void processNonKeyOptions() {
		// Create a copy so we can track which flags we have processed
		final Set<SqrlRequestOpt> unsupportedOpList = new HashSet<>(sqrlClientRequest.getOptList());

		// Remove the key opts from the list since they are processed in SqrlServerOperations
		for (final SqrlRequestOpt keyOpt : SqrlRequestOpt.getKeyOpts()) { // TODO: rename core  opts?
			unsupportedOpList.remove(keyOpt);
		}

		// The absence of given flags means they should be disabled. So loop through all known flags and take the
		// appropriate action
		for (final SqrlIdentityFlag flag : SqrlIdentityFlag.values()) {
			final SqrlRequestOpt opt = flag.getSqrlClientOpt();
			if (flag.hasOptEquivalent() && opt.isPersist()) {
				updateOptValueAsNeeded(flag, opt);
				// Return type of remove is irrelevant since absence of opt means disable
				unsupportedOpList.remove(flag.getSqrlClientOpt());
			}
		}

		// Some flags require special processing and were not handled above
		// CPS is a request flag that is per request
		unsupportedOpList.remove(SqrlRequestOpt.cps);
		unsupportedOpList.remove(SqrlRequestOpt.noiptest);

		// What's left is unknown or unsupported to us
		if (!unsupportedOpList.isEmpty()) {
			logger.warn("{}The SQRL client option(s) are not yet supported by the library: {}",
					logHeader, unsupportedOpList);
		}

	}

	private void processCommand() throws SqrlException {
		switch (command) {
		case QUERY:
			// Nothing to do
			return;
		case IDENT:
			processIdentCommand();
			return;
		case ENABLE:
			final Boolean sqrlEnabledForIdentity = sqrlPersistence.fetchSqrlFlagForIdentity(sqrlIdk,
					SqrlIdentityFlag.SQRL_AUTH_ENABLED);
			if (sqrlEnabledForIdentity == null || !sqrlEnabledForIdentity.booleanValue()) {
				if (sqrlClientRequest.containsUrs()) {
					sqrlPersistence.setSqrlFlagForIdentity(sqrlIdk, SqrlIdentityFlag.SQRL_AUTH_ENABLED, true);
				} else {
					throw new SqrlInvalidRequestException(
							logHeader + "Request was to enable SQRL but didn't contain urs signature");
				}
			} else {
				logger.warn("{}Received request to ENABLE but it already is");
			}
			return;
		case DISABLE:
			sqrlPersistence.setSqrlFlagForIdentity(sqrlIdk, SqrlIdentityFlag.SQRL_AUTH_ENABLED, false);
			return;
		case REMOVE:
			if (sqrlClientRequest.containsUrs()) {
				sqrlPersistence.deleteSqrlIdentity(sqrlIdk);
			} else {
				throw new SqrlInvalidRequestException(
						logHeader + "Request was to remove SQRL but didn't contain urs signature");
			}
			return;
		default:
			// This should have been handled prior to here
			throw new SqrlClientRequestProcessingException(
					logHeader + "Don't know how to process SQRL command " + command);
		}
	}

	private void processIdentCommand() throws SqrlException {
		if (!sqrlInternalUserState.idExistsInPersistence()) {
			// First time seeing this SQRL identity, store it and enable it
			sqrlPersistence.createAndEnableSqrlIdentity(sqrlIdk);
			sqrlPersistence.storeSqrlDataForSqrlIdentity(sqrlIdk, sqrlClientRequest.getKeysToBePersisted());
		}
		final boolean sqrlEnabledForIdentity = sqrlPersistence.fetchSqrlFlagForIdentity(sqrlIdk,
				SqrlIdentityFlag.SQRL_AUTH_ENABLED);
		if (!sqrlEnabledForIdentity) {
			sqrlInternalUserState = SqrlInternalUserState.DISABLED;
		} else if (sqrlInternalUserState == SqrlInternalUserState.PIDK_EXISTS) {
			sqrlPersistence.updateIdkForSqrlIdentity(sqrlClientRequest.getKey(pidk), sqrlIdk);
			logger.info("{}User SQRL authenticated, updating idk={} and to replace pidk",
					logHeader, sqrlIdk);
			// TODO_AUDIT
		} else if (sqrlInternalUserState == SqrlInternalUserState.IDK_EXISTS) {
			// TODO_AMBIGUOUS: do we really overwrite existing data, or only if they are new?
			sqrlPersistence.storeSqrlDataForSqrlIdentity(sqrlIdk, sqrlClientRequest.getKeysToBePersisted());
			sqrlInternalUserState = SqrlInternalUserState.IDK_EXISTS;
			// TODO_AUDIT
			logger.info("{}User SQRL authenticated idk={}", logHeader, sqrlIdk);
		}
		final boolean cpsRequested = sqrlClientRequest.getOptList().contains(SqrlRequestOpt.cps);
		if (cpsRequested) {
			if (sqrlconfig.isEnableCps()) {
				// Tell the browser to stop polling for a response
				final SqrlCorrelator sqrlCorrelator = sqrlPersistence.fetchSqrlCorrelatorRequired(correlator);
				// Setting AuthenticationStatus to CPS drives the CPS logic in the rest of this code
				sqrlCorrelator.setAuthenticationStatus(SqrlAuthenticationStatus.AUTHENTICATED_CPS);
			} else {
				// Per the SQRL spec, servers are not required to support cps, but the client can always request it
				logger.debug("cps was requested but is disabled in sqrlconfig.  Continuing with browser sign on");
			}
		}
		sqrlPersistence.userAuthenticatedViaSqrl(sqrlIdk, correlator);
	}
}