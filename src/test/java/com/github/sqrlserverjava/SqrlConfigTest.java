package com.github.sqrlserverjava;

import static junit.framework.TestCase.assertEquals;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.sqrlserverjava.enums.SqrlQrCodeImageFormat;

public class SqrlConfigTest {
	private static JAXBContext jaxbContext = null;

	@BeforeClass
	public static void beforeClass() throws Exception {
		jaxbContext = JAXBContext.newInstance(SqrlConfig.class);
	}

	private static final String EXPECTED_TEST_MARSHALL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><sqrlConfig><aesKeyBase64>KCI0BLITcZiR8b8hp3VWtA==</aesKeyBase64><authSyncCheckInMillis>500</authSyncCheckInMillis><backchannelServletPath>/sqrlbc</backchannelServletPath><cleanupTaskExecInMinutes>15</cleanupTaskExecInMinutes><clientAuthStateUpdaterClass>com.MyClass</clientAuthStateUpdaterClass><cookiePath>/</cookiePath><correlatorCookieName>sqrlcorrelator</correlatorCookieName><enableCps>true</enableCps><firstNutCookieName>sqrlfirstnut</firstNutCookieName><nutValidityInSeconds>900</nutValidityInSeconds><qrCodeImageFormat>PNG</qrCodeImageFormat><sqrlLoginServletPath>/sqrllogin</sqrlLoginServletPath><sqrlNutTokenFormat>2</sqrlNutTokenFormat><sqrlPersistenceFactoryClass>com.github.sqrlserverjava.persistence.SqrlJpaPersistenceFactory</sqrlPersistenceFactoryClass></sqrlConfig>";

	/**
	 * Basic test to ensure we don't break {@link SqrlConfig} JAXB marshalling by trying to do something illegal (try
	 * marshal an interface, etc)
	 */
	@Test
	public void testMarshall() throws Exception {
		final byte[] expectedKeyBytes = new byte[] { 40, 34, 52, 4, -78, 19, 113, -104, -111, -15, -65, 33, -89,
				117, 86, -76 };
		String aesKeyBase64 = Base64.getEncoder().encodeToString(expectedKeyBytes);
		final SqrlConfig config = new SqrlConfig();
		config.setClientAuthStateUpdaterClass("com.MyClass");
		config.setBackchannelServletPath("/sqrlbc");
		config.setAesKeyBase64(aesKeyBase64);
		final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		final StringWriter writer = new StringWriter();
		jaxbMarshaller.marshal(config, writer);
		assertEquals(EXPECTED_TEST_MARSHALL, writer.toString());
	}

	@Test
	public void testUnmarshall() throws Exception {
		try (BufferedReader reader = new BufferedReader(new StringReader(EXPECTED_TEST_MARSHALL))) {
			final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			final SqrlConfig config = (SqrlConfig) jaxbUnmarshaller.unmarshal(reader);
			assertEquals(900, config.getNutValidityInSeconds());
			assertEquals(SqrlQrCodeImageFormat.PNG, config.getQrCodeImageFormat());
			assertEquals("/sqrlbc", config.getBackchannelServletPath());
			assertEquals("KCI0BLITcZiR8b8hp3VWtA==", config.getAesKeyBase64());
		}
	}
}
