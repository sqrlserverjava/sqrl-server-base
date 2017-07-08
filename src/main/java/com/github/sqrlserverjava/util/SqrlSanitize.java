package com.github.sqrlserverjava.util;

import com.github.sqrlserverjava.exception.SqrlInvalidDataException;

/**
 * Helper class to ensure data coming from the Internet is sanitized against security vulnerabilities such as XSS. If
 * any issues are found, {@link SqrlInvalidDataException} is thrown to halt the transaction
 *
 * @author Dave Badia
 *
 */
public class SqrlSanitize {

	/**
	 * Performs basic security checks for size and character format for all tokens used in this library: nut,
	 * correlator, http params, etc
	 *
	 * @param data
	 *            the data to be examined
	 * @throws SqrlInvalidDataException
	 *             if data validation fails
	 */
	public static void inspectIncomingSqrlData(final String data) throws SqrlInvalidDataException {
		if (SqrlUtil.isBlank(data)) {
			return;
		}
		if(data.length() > SqrlConstants.MAX_SQRL_TOKEN_SIZE) {
			throw new SqrlInvalidDataException(
					"Data size of " + data.length() + " exceeded max size of " + SqrlConstants.MAX_SQRL_TOKEN_SIZE);
		} else if (!SqrlUtil.REGEX_PATTERN_REGEX_BASE64_URL.matcher(data).matches()) {
			throw new SqrlInvalidDataException("Data failed base64url validation: '" + data + "'");
		}
	}
}
