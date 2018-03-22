package com.github.sqrlserverjava.backchannel.nut;

import java.net.InetAddress;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlConfigOperations;
import com.github.sqrlserverjava.backchannel.SqrlTifFlag;
import com.github.sqrlserverjava.exception.SqrlClientRequestProcessingException;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.util.SqrlUtil;

// @formatter:off
/**
 * Factory pattern class for marshaling and unmarshaling of SQRL "nut" tokens. The SQRL spec suggests a possible format,
 * but does not mandate the format. This library supports multiple layouts of the token to support the following:
 * 
 * 1. a token format matching the size (but not the data layout) of the format suggested by the spec.  This is not 
 * preferred but is retained in case clients have issues processing our larger, preferred format.  Note this format requires
 * server side state (that is, database entries) when displaying the login page.  This format is encrypted but is <b>not</b> signed.
 * 
 *  2. a token format which does not require server side state that is both encrypted and signed
 * 
 * @author Dave Badia
 *
 */
// @formatter:on
public class SqrlNutTokenFactory {
	private static final Logger	logger		= LoggerFactory.getLogger(SqrlNutTokenFactory.class);
	private static int			formatToUse	= -1;

	private SqrlNutTokenFactory() {
		// Factory
	}

	public static SqrlNutToken unmarshal(String nutTokenString, SqrlConfigOperations configOperations)
			throws SqrlClientRequestProcessingException {
		byte[] tokenBytes = SqrlUtil.base64UrlDecodeDataFromSqrlClient(nutTokenString);

		int formatIdOfNutToken = -1;
		if (tokenBytes.length == 16) {
			// If the decoded token is 16 bytes, it is the legacy format which does not embed any format info
			formatIdOfNutToken = 0;
		} else {
			// Newer token, extract the format ID to determine how to parse it
			formatIdOfNutToken = SqrlNutToken.buildFormatId(tokenBytes[0]);
		}
		if (formatIdOfNutToken == -1) {
			throw new SqrlClientRequestProcessingException(SqrlTifFlag.COMMAND_FAILED, null,
					"Could not detemrine format ID of SqrlNutToken=", nutTokenString);
		} else if (formatIdOfNutToken != formatToUse) {
			logger.warn(
					"Unmarshalling SqrlNutToken with format {} which is different than preferred format of {} SqrlNutToken={}",
					formatIdOfNutToken, formatToUse, nutTokenString);
		}

		if(SqrlNutTokenLegacyFormat.FORMAT_ID == formatIdOfNutToken) {
			return new SqrlNutTokenLegacyFormat(configOperations, nutTokenString);
		}else if(SqrlNutTokenSingleBlockFormat.FORMAT_ID == formatIdOfNutToken) {
			return new SqrlNutTokenSingleBlockFormat(configOperations, nutTokenString);
		}else if(SqrlNutTokenEmbedded.FORMAT_ID == formatIdOfNutToken) {
			return new SqrlNutTokenEmbedded(configOperations, nutTokenString);
		} else {
			throw new SqrlClientRequestProcessingException(SqrlTifFlag.COMMAND_FAILED, null,
					"Cant create SqrlNutToken with formatid=", formatIdOfNutToken);
		}
	}

	public static SqrlNutToken buildNut(SqrlConfig config, SqrlConfigOperations configOperations, URI backchannelUri,
			InetAddress browserIPAddress) throws SqrlException {
		if(formatToUse < 0) {
			formatToUse = config.getSqrlNutTokenFormat();
		}
		
		if(formatToUse == SqrlNutTokenLegacyFormat.FORMAT_ID) {
			return new SqrlNutTokenLegacyFormat(browserIPAddress, configOperations, System.currentTimeMillis());
		} else if (formatToUse == SqrlNutTokenSingleBlockFormat.FORMAT_ID) {
			return new SqrlNutTokenSingleBlockFormat(browserIPAddress, configOperations, System.currentTimeMillis());
		} else if (formatToUse == SqrlNutTokenEmbedded.FORMAT_ID) {
			// TODO: will have to perform additional refactoring to make use of these fields
			// for now use dummy values so we represent the possible size of the real data (even though we will likely
			// reduce it)
			String dummyCorrelator = "";
			String dummyLoginUrl = "1";
			return new SqrlNutTokenEmbedded(browserIPAddress, configOperations, System.currentTimeMillis(),
					dummyCorrelator, dummyLoginUrl);
		} else {
			throw new SqrlException("Unknown SqrlNutToken format ID of ", formatToUse);
		}
	}
}
