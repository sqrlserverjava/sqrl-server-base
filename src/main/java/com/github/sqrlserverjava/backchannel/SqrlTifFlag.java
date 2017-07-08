package com.github.sqrlserverjava.backchannel;

/**
 * Enumeration of the SQRL tif flags, see https://www.grc.com/sqrl/semantics.htm
 * 
 * @author Dave Badia
 *
 */
public enum SqrlTifFlag {
	// @formatter:off
	/**
	 * (Current) ID match: When set, this bit indicates that the web server has found an identity association for the
	 * user based upon the default (current) identity credentials supplied by the client: the IDentity Key (idk) and the
	 * IDentity Signature (ids).
	 */
	CURRENT_ID_MATCH 		(0x01),
	/**
	 * Previous ID match: When set, this bit indicates that the web server has found an identity association for the
	 * user based upon the previous identity credentials supplied by the client in the previous IDentity Key (pidk) and
	 * the previous IDentity Signature (pids). Note: When neither of the ID match bits are set, none of the identity
	 * credentials supplied by the client are known to the web server.
	 */
	PREVIOUS_ID_MATCH		(0x02),
	/**
	 * IPs matched: When set, this bit indicates that the IP address of the entity which requested the initial logon web
	 * page containing the SQRL link URL (and probably encoded into the SQRL link URL's “nut”) is the same IP address
	 * from which the SQRL client's query was received for this reply. Note that the server must retain the IP embedded
	 * in, or associated with, the original SQRL URL, not any subsequent query, and compare the current query IP against
	 * that original IP.
	 */
	IPS_MATCHED				(0x04),
	/**
	 * SQRL disabled: When set, this bit indicates that SQRL authentication for this identity has previously been
	 * disabled. While this bit is set, the “ident” command and any attempt at authentication will fail. This bit can
	 * only be reset, and the identity re-enabled for authentication, by the client issuing an “enable” command signed
	 * by the unlock request signature (urs) for the identity known to the server. Since this signature requires the
	 * presence of the identity's RescueCode, only SQRL's strongest identity authentication is permitted to re-enable a
	 * disabled identity.
	 */
	SQRL_DISABLED			(0x08),
	/**
	 * Function(s) not supported: This bit indicates that the client requested one or more SQRL functions (through
	 * command verbs) that the server does not currently support. The client will likely need to advise its user that
	 * whatever they were trying to do is not possible at the target website. The SQRL server will fail this query, thus
	 * also setting the “40h” Command Failed bit.
	 */
	FUNCTIONS_NOT_SUPPORTED	(0x10),
	/**
	 * Transient error: The server replies with this bit set to indicate that the client's signature(s) are correct, but
	 * something about the client's query prevented the command from completing. This is the server's way of instructing
	 * the client to retry and reissue the immediately previous command using the fresh ‘nut=’ crypto material and
	 * ‘qry=’ url the server will have also just returned in its reply. Although we don't want to overly restrict the
	 * specification of this error, the trouble is almost certainly static, expired, or previously used nut= or qry=
	 * data. Thus, reissuing the previous command under the newly supplied server parameters would be expected to
	 * succeed. The “0x40” “Command failed” bit (shown next) will also be set since the client's command will not have
	 * been processed.
	 */
	TRANSIENT_ERROR			(0x20),
	/**
	 * Command failed: When set, this bit indicates that the web server has encountered a problem successfully
	 * processing the client's query. In any such case, no change will be made to the user's account status. All SQRL
	 * server-side actions are atomic. This means that either everything succeeds or nothing is changed. This is
	 * important since clients can request multiple updates and changes at once.
	 *
	 * If this bit is set without the 80h bit set (see below) the trouble was not with the client's provided data,
	 * protocol, etc. but with some other aspect of completing the client's request. With the exception of the following
	 * “Client failure” status bit, the SQRL semantics do not attempt to enumerate every conceivable web server failure
	 * reason. The web server is free to use its “ask” feature to explain the problem to the client's user.
	 */
	COMMAND_FAILED			(0x40),
	/**
	 * Client failure: This bit is set by the server when some aspect of the client's submitted query ‑ other than
	 * expired but otherwise valid transaction state information ‑ was incorrect and prevented the server from
	 * understanding and/or completing the requested action. This could be the result of a communications error, a
	 * mistake in the client's SQRL protocol, a signature that doesn't verify, or required signatures for the requested
	 * actions which are not present. And more specifically, this is NOT an error that the server knows would likely be
	 * fixed by having the client silently reissue it previous command using updated entropy . . . although that might
	 * still be the first recourse for the client. Since any such client failure will also result in a failure of the
	 * command, the 40h bit will also be set.
	 */
	CLIENT_FAILURE			(0x80),
	/**
	 * Bad ID Association: This bit is set by the server when a SQRL identity which may be associated with the query nut
	 * does not match the SQRL ID used to submit the query. If the server is maintaining session state, such as a logged
	 * on session, it may generate SQRL query nuts associated with that logged-on session's SQRL identity. If it then
	 * receives a SQRL query using that nut, but issued with a different SQRL identity, it should fail the command
	 * (setting both the 0x40 and 0x80 bits) and also return this 0x100 error bit so that the client may inform its user
	 * that the wrong SQRL identity was used with a nut that was already associated with a different identity.
	 */
	BAD_ID_ASSOCIATION		(0x100),

	/**
	 * DEPRECATED NO LONGER DEFINED - DO NOT USE
	 *
	 * @deprecated per SQRL spec: NO LONGER DEFINED - DO NOT USE
	 */
	@Deprecated
	DEPRECATED_INVALID_LINK_ORIGIN (0x200),

	/**
	 * DEPRECATED NO LONGER DEFINED - DO NOT USE
	 *
	 * @deprecated per SQRL spec: NO LONGER DEFINED - DO NOT USE
	 */
	@Deprecated
	DEPRECATED_SUPRESS_SFN_ORIGIN (0x400),

	;
	// @formatter:on

	private final int mask;

	private SqrlTifFlag(final int mask) {
		this.mask = mask;
	}

	public int getMask() {
		return mask;
	}
}
