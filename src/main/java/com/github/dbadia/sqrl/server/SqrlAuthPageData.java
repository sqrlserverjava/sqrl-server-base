package com.github.dbadia.sqrl.server;

import java.io.ByteArrayOutputStream;

import com.github.dbadia.sqrl.server.backchannel.SqrlNutToken;

/**
 * Encapsulates the SQRL related data that needs to be displayed for a SQRL login to occur
 * 
 * @author Dave Badia
 *
 */
public class SqrlAuthPageData {
	private final String url;
	private final ByteArrayOutputStream qrBaos;
	private final SqrlNutToken nut;
	private final String correlator;

	public SqrlAuthPageData(final String url, final ByteArrayOutputStream qrBaos, final SqrlNutToken nut,
			final String correlator) {
		this.url = url;
		this.qrBaos = qrBaos;
		this.nut = nut;
		this.correlator = correlator;
	}

	public String getUrl() {
		return url;
	}

	public ByteArrayOutputStream getQrCodeOutputStream() {
		return qrBaos;
	}

	public SqrlNutToken getNut() {
		return nut;
	}

	public String getCorrelator() {
		return correlator;
	}

}
