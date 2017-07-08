package com.github.sqrlserverjava.enums;

/**
 * Represents the state of a SQRL user within our application
 * 
 * @author Dave Badia
 *
 */
public enum SqrlInternalUserState {
	// @formatter:off
	IDK_EXISTS,
	/**
	 * Indicates that the server found a match for the given pidk
	 */
	PIDK_EXISTS,
	NONE_EXIST,
	DISABLED,
	;

	/**
	 * @return true if the user state indicates that this user is registered on the system
	 */
	public boolean idExistsInPersistence() {
		return this == IDK_EXISTS || this == PIDK_EXISTS;
	}
}
