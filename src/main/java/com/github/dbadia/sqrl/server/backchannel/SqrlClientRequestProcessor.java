package com.github.dbadia.sqrl.server.backchannel;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlFlag;
import com.github.dbadia.sqrl.server.SqrlPersistence;
import com.github.dbadia.sqrl.server.SqrlServerOperations;
import com.github.dbadia.sqrl.server.backchannel.SqrlTif.SqrlTifBuilder;
import com.github.dbadia.sqrl.server.exception.SqrlDisabledUserException;
import com.github.dbadia.sqrl.server.exception.SqrlInvalidRequestException;
import com.github.dbadia.sqrl.server.util.SqrlException;

public class SqrlClientRequestProcessor {
	private static final Logger logger = LoggerFactory.getLogger(SqrlServerOperations.class);

	// TODO: change COMMAND_ constants to enum
	private static final String	COMMAND_QUERY	= "query";
	private static final String	COMMAND_IDENT	= "ident";
	private static final String	COMMAND_DISABLE	= "disable";
	private static final String	COMMAND_ENABLE	= "enable";
	private static final String	COMMAND_REMOVE	= "remove";

	private final SqrlClientRequest			sqrlClientRequest;
	private final String			sqrlIdk;
	private final SqrlTifBuilder				tifBuilder;
	private final String					command;
	private final String					logHeader;
	private final String			correlator;
	private final SqrlPersistence	sqrlPersistence;

	public SqrlClientRequestProcessor(final SqrlClientRequest sqrlClientRequest,
			final SqrlPersistence sqrlPersistence, final SqrlTifBuilder tifBuilder) {
		super();
		this.logHeader = SqrlLoggingUtil.getLogHeader();
		this.sqrlPersistence = sqrlPersistence;
		this.sqrlClientRequest = sqrlClientRequest;
		this.command = sqrlClientRequest.getClientCommand();
		this.sqrlIdk = sqrlClientRequest.getIdk();
		this.tifBuilder = tifBuilder;
		this.correlator = sqrlClientRequest.getCorrelator();
	}
	// TODO: move this to whereever persistence is created
	// Per the spec, SQRL transactions are atomic; so we create our persistence here and only commit after all
	// processing is completed successfully

	public boolean processClientCommand() throws SqrlException {
		final boolean idkExistsInDataStore = sqrlPersistence.doesSqrlIdentityExistByIdk(sqrlIdk);
		// Set IDK /PIDK Tifs
		if (idkExistsInDataStore) {
			tifBuilder.addFlag(SqrlTif.TIF_CURRENT_ID_MATCH);
		} else if (sqrlClientRequest.hasPidk() // PIDK check TODO: move too identity command area
				&& sqrlPersistence.doesSqrlIdentityExistByIdk(sqrlClientRequest.getPidk())) {
			sqrlPersistence.updateIdkForSqrlIdentity(sqrlClientRequest.getPidk(), sqrlIdk);
			// TODO audit
			tifBuilder.addFlag(SqrlTif.TIF_PREVIOUS_ID_MATCH);
		}

		final boolean idkExistsNow = processCommand(idkExistsInDataStore);
		if (!COMMAND_REMOVE.equals(command)) {
			processNonSukOptions();
		}
		return idkExistsNow;
	}

	private void updateOptValueAsNeeded(final SqrlFlag flag, final SqrlClientOpt opt) {
		if (opt != null && !opt.isNonQueryOnly() || !COMMAND_QUERY.equals(sqrlClientRequest.getClientCommand())) {
			final boolean clientValue = sqrlClientRequest.getOptList().contains(opt);
			final boolean dbValue = sqrlPersistence.fetchSqrlFlagForIdentity(sqrlIdk, flag);
			if (clientValue != dbValue) { // update it
				logger.debug("{}Updating SQRL flag {} from {} to {}", SqrlLoggingUtil.getLogHeader(), opt, dbValue,
						clientValue);
				sqrlPersistence.setSqrlFlagForIdentity(sqrlIdk, flag, clientValue);
				// TODO: audit, client updated value to clientSet
			}
		}
	}

	private void processNonSukOptions() {
		// Create a copy so we can track which flags we have processed
		final List<SqrlClientOpt> optList = sqrlClientRequest.getOptList();

		// The absence of given flags means they should be disabled. So loop through all known flags and take the
		// appropriate action
		for (final SqrlFlag flag : SqrlFlag.values()) {
			if (flag.hasOptEquivalent()) {
				final SqrlClientOpt opt = flag.getSqrlClientOpt();
				updateOptValueAsNeeded(flag, opt);
				// Remove the item from the list since we have processed it
				if (!optList.remove(flag.getSqrlClientOpt())) {
					logger.warn("{}Tried to remove SqrlClientOpt {} but it wasn't in list {}",
							SqrlLoggingUtil.getLogHeader(), opt, optList);
				}
			}
		}

		// Some flags require special processing and were not handled above
		if (!optList.isEmpty()) {
			logger.warn("{}The SQRL client option(s) are not yet supported by the library: {}",
					SqrlLoggingUtil.getLogHeader(), optList);
		}

	}

	private boolean processCommand(final boolean idkExistsInDataStoreParam) throws SqrlException {
		final boolean idkExistsInDataStore = idkExistsInDataStoreParam;
		if (COMMAND_QUERY.equals(command)) {
			// Nothing to do
		} else if (COMMAND_IDENT.equals(command)) {
			processIdentCommand(idkExistsInDataStoreParam);
		} else if (COMMAND_ENABLE.equals(command)) {
			final Boolean sqrlEnabledForIdentity = sqrlPersistence.fetchSqrlFlagForIdentity(sqrlIdk,
					SqrlFlag.SQRL_AUTH_ENABLED);
			if (sqrlEnabledForIdentity == null || !sqrlEnabledForIdentity.booleanValue()) {
				if (sqrlClientRequest.containsUrs()) {
					sqrlPersistence.setSqrlFlagForIdentity(sqrlIdk, SqrlFlag.SQRL_AUTH_ENABLED, true);
				} else {
					throw new SqrlInvalidRequestException(SqrlLoggingUtil.getLogHeader()
							+ "Request was to enable SQRL but didn't contain urs signature");
				}
			} else {
				logger.warn("{}Received request to ENABLE but it already is");
			}
		} else if (COMMAND_REMOVE.equals(command)) {
			if (sqrlClientRequest.containsUrs()) {
				sqrlPersistence.deleteSqrlIdentity(sqrlIdk);
			} else {
				throw new SqrlInvalidRequestException(
						SqrlLoggingUtil.getLogHeader() + "Request was to enable SQRL but didn't contain urs signature");
			}
		} else if (COMMAND_DISABLE.equals(command)) {
			sqrlPersistence.setSqrlFlagForIdentity(sqrlIdk, SqrlFlag.SQRL_AUTH_ENABLED, false);
		} else {
			tifBuilder.addFlag(SqrlTif.TIF_FUNCTIONS_NOT_SUPPORTED);
			tifBuilder.addFlag(SqrlTif.TIF_COMMAND_FAILED);
			throw new SqrlException(SqrlLoggingUtil.getLogHeader() + "Unsupported client command " + command, null);
		}
		return idkExistsInDataStore;
	}

	private boolean processIdentCommand(final boolean idkExistsInDataStoreParam) throws SqrlException {
		boolean idkExistsInDataStore = idkExistsInDataStoreParam;
		if (!idkExistsInDataStore) {
			// First time seeing this SQRL identity, store it and enable it
			// TODO: remove 2nd arg if it's always null? or pass request.getKeysToBeStored()
			sqrlPersistence.createAndEnableSqrlIdentity(sqrlIdk, Collections.emptyMap());
			sqrlPersistence.storeSqrlDataForSqrlIdentity(sqrlIdk, sqrlClientRequest.getKeysToBeStored());
		}
		final boolean sqrlEnabledForIdentity = sqrlPersistence.fetchSqrlFlagForIdentity(sqrlIdk,
				SqrlFlag.SQRL_AUTH_ENABLED);
		if (!sqrlEnabledForIdentity) {
			// TODO: move this setting to catch
			tifBuilder.addFlag(SqrlTif.TIF_SQRL_DISABLED);
			tifBuilder.addFlag(SqrlTif.TIF_COMMAND_FAILED);
			throw new SqrlDisabledUserException(
					SqrlLoggingUtil.getLogHeader() + "SQRL authentication is disabled for this user");
		} else { // sqrlEnabledForIdentity
			// TODO: do we really overwrite existing data, or only if they are new?
			sqrlPersistence.storeSqrlDataForSqrlIdentity(sqrlIdk, sqrlClientRequest.getKeysToBeStored());
			// TODO: is this right? set to true?
			idkExistsInDataStore = true;
			logger.info("{}User SQRL authenticated idk={}", SqrlLoggingUtil.getLogHeader(), sqrlIdk);

			final boolean cpsRequested = sqrlClientRequest.getOptList().contains(SqrlClientOpt.cps);
			final boolean cpsEnabled = false; // TODO: have SSO pass SqrlCpsGenerator instances and check for non-null
			if (cpsRequested && cpsEnabled) {
				// TODO: do it
			} else { // Not requested or not enabled
				if (cpsRequested && !cpsEnabled) {
					logger.info("{} CPS requested but it is not enabled", SqrlLoggingUtil.getLogHeader());
				}
				sqrlPersistence.userAuthenticatedViaSqrl(sqrlIdk, correlator);
			}
		}
		return idkExistsInDataStore;
	}
}