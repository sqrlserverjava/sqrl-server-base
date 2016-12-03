package com.github.dbadia.sqrl.server.enums;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.dbadia.sqrl.server.util.SqrlServerSideKey;

/**
 * Enum representing the valid signature types of the SQRL spec. The spec refers to all of these in lower case, so we
 * break java convention and do the same for readability
 */
public enum SqrlSignatureType {
	// @formatter:off
	ids,
	urs,
	;
	// @formatter:on

	public static final Map<SqrlSignatureType, SqrlServerSideKey> getSignatureToKeyParamTable() {
		final Map<SqrlSignatureType, SqrlServerSideKey> map = new ConcurrentHashMap<>();
		map.put(ids, SqrlServerSideKey.idk);
		// Think this is wrong... believe urs is a combo of keys? will circle back when we add urs support
		map.put(urs, SqrlServerSideKey.suk);
		return map;
	}
}
