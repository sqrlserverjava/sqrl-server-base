package com.github.dbadia.sqrl.server.util;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public enum SqrlServerSideKey {
	/**
	 * The spec refers to all of these in lower case, so we will too. It breaks java convention but is more readable
	 */
	// @formatter:off
	idk(false),
	pidk(false),
	suk(true),
	vuk(true);

	// @formatter:on
	/**
	 * Whether the given key needs to be associated with a SQRL Identity and persisted by the SQRL server
	 */
	private final boolean needsBePersisted;

	SqrlServerSideKey(final boolean shouldBeStored) {
		this.needsBePersisted = shouldBeStored;
	}

	private static final Map<String, SqrlServerSideKey> LOOKUP_TABLE;
	static {
		final Map<String, SqrlServerSideKey> tempTable = new TreeMap<>();
		for (final SqrlServerSideKey aKey : values()) {
			tempTable.put(aKey.toString(), aKey);
		}
		LOOKUP_TABLE = Collections.unmodifiableMap(tempTable);
	}

	/**
	 * A version of {@code values} which is case-insensitive and returns null when not found instead of throwing an
	 * exception
	 *
	 * @return the key for this name or null
	 */
	public static SqrlServerSideKey valueOfOrNull(final String toFind) {
		return LOOKUP_TABLE.get(toFind.toLowerCase());
	}

	/**
	 * Whether the given key, if sent in a SQRL client request, must be associated with the users SQRL Identity and
	 * persisted
	 *
	 * @return if the key should be persisted
	 */
	public boolean needsToBePersisted() {
		return needsBePersisted;
	}

	/**
	 * Checks to see if a given name from the {@CLIENT} param is a key that needs to be associated with the users SQRL
	 * Identity and persisted
	 *
	 * @param dataName
	 *            name of a name/value pair passed in the {@CLIENT} param
	 * @return true if it represents a key and that key should be persisted
	 */
	public static boolean isRequestDataAKeyThatNeedsToBePersisted(final String dataName) {
		for (final SqrlServerSideKey aKey : SqrlServerSideKey.values()) {
			if (aKey.toString().equals(dataName) && SqrlServerSideKey.valueOf(dataName).needsToBePersisted()) {
				return true;
			}
		}
		return false;
	}
}
