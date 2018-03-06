package com.github.sqrlserverjava.util;

import static com.github.sqrlserverjava.util.SqrlConstants.SQRL_ATOMOSPHERE_LIB_UPDATER_CLASS;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.exception.SqrlConfigSettingException;
import com.github.sqrlserverjava.exception.SqrlIllegalStateException;

public class SqrlConfigHelper {
	private static final Logger	logger	= LoggerFactory.getLogger(SqrlConfigHelper.class);
	private static final String DEFAULT_CONFIG_NAME = "sqrlconfig.xml";
	private static final String SQRL_CONFIG_XSD = "sqrlconfig.xsd";

	private static JAXBContext jaxbContext;

	private SqrlConfigHelper() {
		// util class
	}

	public static SqrlConfig loadFromClasspath() {
		return loadFromClasspath(DEFAULT_CONFIG_NAME);
	}

	public static SqrlConfig loadFromClasspath(final String sqrlConfigXmlFileName) {
		final URL url = findResource(sqrlConfigXmlFileName);
		final String sqrlConfigXml = loadDataFromUrl(url);
		try {
			final SqrlConfig sqrlConfig = validateAgainstSchemaThenUnmarshall(sqrlConfigXml, url);
			initializeSqrlConfig(sqrlConfig);
			return sqrlConfig;
		} catch (final Exception e) {
			throw new SqrlIllegalStateException("Error unmarshalling SQRL config, schema validation failed", e);
		}
	}

	private static SqrlConfig validateAgainstSchemaThenUnmarshall(final String sqrlConfigXml, final URL url)
			throws IOException {
		final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		final String xsdData = loadDataFromUrl(findResource(SQRL_CONFIG_XSD));
		try (InputStream xmlIs = buildInputStreamFromString(sqrlConfigXml); InputStream xsdIs = buildInputStreamFromString(xsdData)) {
			final Schema schema = factory.newSchema(new StreamSource(xsdIs));
			final Validator validator = schema.newValidator();
			validator.validate(new StreamSource(xmlIs));
		} catch (final SAXException e) {
			throw new SqrlIllegalStateException(
					"Error unmarshalling SQRL config at " + url.getPath() + ", schema validation failed", e);
		}
		// Unmarshal
		try (InputStream is = buildInputStreamFromString(sqrlConfigXml)) {
			final Unmarshaller jaxbUnarshaller = loadJaxbContext().createUnmarshaller();
			final SqrlConfig sqrlConfig = (SqrlConfig) jaxbUnarshaller.unmarshal(is);
			return sqrlConfig;
		} catch (final JAXBException e) {
			throw new SqrlIllegalStateException("Error unmarshalling SQRL config from " + url.getPath(), e);
		}
	}

	private static String loadDataFromUrl(final URL url) {
		try {
			final URLConnection conn = url.openConnection();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
				return reader.lines().collect(Collectors.joining("\n"));
			}
		} catch (final IOException e) {
			throw new SqrlIllegalStateException("Error reading data from " + url.getPath(), e);
		}
	}

	private static InputStream buildInputStreamFromString(final String data) {
		return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
	}

	private static URL findResource(final String name) {
		final URL url = SqrlConfigHelper.class.getClassLoader().getResource(name);
		if (url == null) {
			throw new SqrlIllegalStateException("Resource '" + name + "' not found on classpath");
		}
		logger.info("Found resource {} at {}", name, url.getPath());
		return url;
	}
	
	public static byte[] getAESKeyBytes(SqrlConfig sqrlConfig) {
		String aesKeyBase64 = sqrlConfig.getAesKeyBase64();
		byte[] aesKeyBytes = null;
		if(SqrlUtil.isBlank(aesKeyBase64)) {
			logger.warn("No AES key set, generating new one");
			aesKeyBytes = new byte[SqrlConstants.AES_KEY_LENGTH];
			sqrlConfig.getSecureRandom().nextBytes(aesKeyBytes);
			sqrlConfig.setAesKeyBase64(Base64.getEncoder().encodeToString(aesKeyBytes));
		} else {
			try {
				aesKeyBytes = Base64.getDecoder().decode(aesKeyBase64);
			} catch (IllegalArgumentException e) {
				throw new SqrlConfigSettingException("Error base64 decoding SqrlConfig AES key", e);
			}
			if (aesKeyBytes.length != SqrlConstants.AES_KEY_LENGTH) {
				throw new SqrlConfigSettingException("SqrlConfig AES key must be " + SqrlConstants.AES_KEY_LENGTH
						+ " bytes, found " + aesKeyBytes.length);
			}
		}
		return aesKeyBytes;
	}

	/**
	 * Populates any optional data on the given SqrlConfig object
	 *
	 * @param sqrlConfig
	 */
	private static void initializeSqrlConfig(final SqrlConfig sqrlConfig) {
		// Set secure random if it is null
		if (sqrlConfig.getSecureRandom() == null) {
			sqrlConfig.setSecureRandom(new SecureRandom());
		}
		// If clientAuthStateUpdatedClass was not explicitly set and the sqrl-sever-atomosphere library
		// is present, then use the sqrl-sever-atomosphere librarys updater class
		if(sqrlConfig.getClientAuthStateUpdaterClass() == null && SqrlUtil.isClassOnClasspath(SQRL_ATOMOSPHERE_LIB_UPDATER_CLASS)) {
			sqrlConfig.setClientAuthStateUpdaterClass(SQRL_ATOMOSPHERE_LIB_UPDATER_CLASS);
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
