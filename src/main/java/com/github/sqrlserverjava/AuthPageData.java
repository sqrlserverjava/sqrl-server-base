package com.github.sqrlserverjava;

import java.io.ByteArrayOutputStream;

import com.github.sqrlserverjava.backchannel.nut.SqrlNutToken0;

/**
 * Encapsulates the SQRL related data that needs to be displayed for a SQRL login to occur
 * 
 * @author Dave Badia
 *
 */
public class AuthPageData {
	private final String				url;
	private final ByteArrayOutputStream	qrBaos;
	private final SqrlNutToken0			nut;
	private final String				correlator;

	public AuthPageData(final String url, final ByteArrayOutputStream qrBaos, final SqrlNutToken0 nut,
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

	public SqrlNutToken0 getNut() {
		return nut;
	}

	public String getCorrelator() {
		return correlator;
	}

	public String getHtmlFileType(final SqrlConfig sqrlConfig) {
		return sqrlConfig.getQrCodeImageFormat().toString().toLowerCase();
	}

}
