package com.github.dbadia.sqrl.server.backchannel;

import java.util.Map;

import com.github.dbadia.sqrl.server.SqrlServerOperations;
import com.github.dbadia.sqrl.server.SqrlUtil;

// @formatter:off
/**
 * Encapsulates a response that will be sent to a SQRL client
 * @author Dave Badia
 *
 */
// @formatter:on
public class SqrlServerReply {
	private static final String VERSION_1 = "1";
	public static final String SEPARATOR = "\r\n";

	private final String nut;
	private final SqrlTif tif;
	private final String queryWithoutNut;
	private final String correlator;
	private final Map<String, String> additionalDataTable;

	// @formatter:off
	/**
	 * From GRC trace
	 * 
	 * ver=1
		nut=AIOdvc0F3RTAOF8pfIV_ug
		tif=5
		qry=/sqrl?nut=AIOdvc0F3RTAOF8pfIV_ug
		suk=mC5wBKDXPkbk3J5ohpkM1ksgv0l996DG2BWWOvOXmF4

	 */
	// @formatter:on
	public SqrlServerReply(final String nut, final SqrlTif tif, final String queryWithoutNut, final String correlator,
			final Map<String, String> additionalDataTable) {
		super();
		this.nut = nut;
		this.tif = tif;
		this.queryWithoutNut = queryWithoutNut;
		this.correlator = correlator;
		this.additionalDataTable = additionalDataTable;
	}

	public String toBase64() {
		final StringBuilder buf = new StringBuilder();
		buf.append("ver=").append(VERSION_1).append(SEPARATOR);
		buf.append("nut=").append(nut).append(SEPARATOR);
		buf.append("tif=").append(tif).append(SEPARATOR);
		buf.append("qry=").append(queryWithoutNut).append("?nut=").append(nut);
		buf.append("&").append(SqrlServerOperations.CORRELATOR_PARAM).append("=").append(correlator).append(SEPARATOR);
		for (final Map.Entry<String, String> entry : additionalDataTable.entrySet()) {
			buf.append(entry.getKey()).append("=").append(entry.getValue()).append(SEPARATOR);
		}
		return SqrlUtil.sqrlBase64UrlEncode(buf.toString());
	}

}
