package com.github.sqrlserverjava.enums;

import java.util.HashSet;
import java.util.Set;

/**
 * Valid values for the opt parameter
 *
 * @author Dave Badia
 */
public enum SqrlRequestOpt {
	/**
	 * “suk” is the abbreviation for Server Unlock Key. The presence of this flag instructs the SQRL server to return
	 * the stored server unlock key (SUK) associated with whichever identity matches the identity supplied by the SQRL
	 * client. The SQRL specification requires the SQRL server to automatically return the account's matching SUK
	 * whenever it is able to anticipate that the client is likely to require it, such as when the server contains a
	 * previous identity key, or when the account is disabled. However, there are instances where the client may know
	 * that it is going to need the stored SUK from the server, such as when it wishes to remove a non-disabled account.
	 * The client could first disable the account to induce the server to return the SUK, but it's simpler for the
	 * client to request the SUK from the server whenever it wants it. It's also conceivable that future extensions of
	 * this specification will incorporate other instances where the server's stored SUK is required for RescueCode
	 * based authentication.
	 *
	 * from https://www.grc.com/sqrl/semantics.htm
	 */
	suk(false, true),

	/**
	 * When present, this option requests the web server to set a flag on this user's account to disable any alternative
	 * non-SQRL authentication capability, such as weaker traditional username and password authentication.
	 *
	 * Users who have become confident of their use of SQRL may ask their client to include this optional request. The
	 * web server should only assume this intention if the option is present in any successful non-query transaction.
	 * Its absence from any successful non-query transaction should immediately reset the flag and the prohibition in
	 * the web server. The web server may, at its option, notice when any change has occurred and explicitly ask the
	 * user to affirm their changed intention.
	 *
	 * from https://www.grc.com/sqrl/semantics.htm
	 */
	sqrlonly(true, false),

	/**
	 * When present, this option requests the web server to set a flag on this user's account to disable any alternative
	 * “out of band” change to this user's SQRL identity, such as traditional and weak “what as your favorite pet's
	 * name” non-SQRL identity authentication.
	 *
	 * Users who have become confident of their use of SQRL may ask their client to include this optional request. The
	 * web server should only assume this intention if the option is present in any successful non-query transaction.
	 * Its absence from any successful non-query transaction should immediately reset the flag and the prohibition in
	 * the web server. The web server may, at its option, notice when any change has occurred and explicitly ask the
	 * user to affirm their changed intention.
	 *
	 * from https://www.grc.com/sqrl/semantics.htm
	 */
	hardlock(true, false),

	/**
	 * “cps” is the abbreviation for Client Provided Session. The presence of this flag alters the system's final
	 * authentication action. In the traditional case when this flag is NOT present, the web server arranges to refresh
	 * the web browser's page or redirect the web browser to an authenticated (typically, logged-on) page. However, for
	 * increased security the “cps” option may be specified by a SQRL client to request the server to abandon its
	 * pending authentication with the user's browser and instead provide the web URL to the client for subsequent
	 * handling. The client then might present its user with a URL link to click, or it might arrange its own private
	 * communications channel to the users web browser so that the user will be taken to the identity-authenticated web
	 * page. This higher security mode has the advantage of completely defeating any known man-in-the-middle attack
	 * where an attacking third-party might have inserted themselves into the loop. This cuts that party out of the
	 * loop, preventing them from obtaining the user's authentication.
	 *
	 * The server provides the authenticated page URL in its “url={url}” response parameter for the final non-query
	 * command.
	 *
	 * from https://www.grc.com/sqrl/semantics.htm
	 */
	cps(true, false),

	;

	private boolean nonQueryOnly;
	private boolean	keyOpt;

	private SqrlRequestOpt(final boolean nonQueryOnly, final boolean keyOpt) {
		this.nonQueryOnly = nonQueryOnly;
		this.keyOpt = keyOpt;
	}

	/**
	 * Per the spec, some flags are to be processed during non-query calls only; account for that here
	 *
	 * @return
	 */
	public boolean isNonQueryOnly() {
		return nonQueryOnly;
	}

	public static Set<SqrlRequestOpt> getKeyOpts() {
		final Set<SqrlRequestOpt> keyOptSet = new HashSet<>();
		for(final SqrlRequestOpt opt : values()) {
			if (opt.keyOpt) {
				keyOptSet.add(opt);
			}
		}
		return keyOptSet;
	}
}
