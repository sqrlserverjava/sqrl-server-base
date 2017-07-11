package com.github.sqrlserverjava.util;

import java.net.URL;
import java.security.SecureRandom;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.exception.SqrlIllegalStateException;

public class SqrlConfigHelper {
	private static final Logger	logger	= LoggerFactory.getLogger(SqrlConfigHelper.class);
	private static JAXBContext	jaxbContext;

	private SqrlConfigHelper() {
		// util class
	}

	public static SqrlConfig loadFromClasspath() {
		return loadFromClasspath("sqrlconfig.xml");
	}

	public static SqrlConfig loadFromClasspath(final String name) {
		final URL url = SqrlConfigHelper.class.getClassLoader().getResource(name);
		if (url == null) {
			throw new SqrlIllegalStateException("SQRL config '" + name + "' not found on classpath");
		}
		logger.info("Found SQRL config file at {}", url.getPath());
		try {
			final Unmarshaller jaxbUnarshaller = loadJaxbContext().createUnmarshaller();
			final SqrlConfig sqrlConfig = (SqrlConfig) jaxbUnarshaller.unmarshal(url);
			validateSqrlConfig(sqrlConfig);
			return sqrlConfig;
		} catch (final JAXBException e) {
			throw new SqrlIllegalStateException("Error unmarshalling SQRL config from " + url.getPath(), e);
		}
	}

	/**
	 * Populates any optional data on the given SqrlConfig object
	 *
	 * @param sqrlConfig
	 */
	private static void validateSqrlConfig(final SqrlConfig sqrlConfig) {
		if (sqrlConfig.getSecureRandom() == null) {
			sqrlConfig.setSecureRandom(new SecureRandom());
		}
	}

	private synchronized static JAXBContext loadJaxbContext() {
		if (jaxbContext == null) {
			try {
				jaxbContext = JAXBContext.newInstance(SqrlConfig.class);
			} catch (final JAXBException e) {
				throw new SqrlIllegalStateException("Error initializing JAXBContext for SqrlConfig.class", e);
			}
		}
		return jaxbContext;
	}
}
