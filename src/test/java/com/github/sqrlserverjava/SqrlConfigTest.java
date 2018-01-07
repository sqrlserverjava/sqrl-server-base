package com.github.sqrlserverjava;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.sqrlserverjava.enums.SqrlQrCodeImageFormat;

import junitx.framework.ArrayAssert;

public class SqrlConfigTest {
	private static JAXBContext jaxbContext = null;

	@BeforeClass
	public static void beforeClass() throws Exception {
		jaxbContext = JAXBContext.newInstance(SqrlConfig.class);
	}

	private static final String EXPECTED_TEST_MARSHALL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><sqrlConfig><clientAuthStateUpdaterClass>com.MyClass</clientAuthStateUpdaterClass><nutValidityInSeconds>900</nutValidityInSeconds><qrCodeFileType>PNG</qrCodeFileType><sqrlPersistenceFactoryClass>com.github.sqrlserverjava.persistence.SqrlJpaPersistenceFactory</sqrlPersistenceFactoryClass><correlatorCookieName>sqrlcorrelator</correlatorCookieName><cleanupTaskExecInMinutes>15</cleanupTaskExecInMinutes><authSyncCheckInMillis>500</authSyncCheckInMillis><firstNutCookieName>sqrlfirstnut</firstNutCookieName><cookiePath>/</cookiePath><sqrlLoginServletPath>/sqrllogin</sqrlLoginServletPath><enableCps>true</enableCps></sqrlConfig>";

	/**
	 * Basic test to ensure we don't break {@link SqrlConfig} JAXB marshalling by trying to do something illegal (try
	 * marshal an interface, etc)
	 */
	@Test
	public void testMarshall() throws Exception {
		final SqrlConfig config = new SqrlConfig();
		config.setClientAuthStateUpdaterClass("com.MyClass");
		final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		final StringWriter writer = new StringWriter();
		jaxbMarshaller.marshal(config, writer);
		System.out.println(writer.toString());
		assertEquals(EXPECTED_TEST_MARSHALL, writer.toString());
	}

	@Test
	public void testUnmarshall() throws Exception {
		final InputStream is = getClass().getResourceAsStream("/sqrlconfig1.xml");
		assertNotNull(is);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			final SqrlConfig config = (SqrlConfig) jaxbUnmarshaller.unmarshal(is);
			assertEquals(600, config.getNutValidityInSeconds());
			assertEquals(SqrlQrCodeImageFormat.PNG, config.getQrCodeFileType());
			assertEquals("/sqrlbc", config.getBackchannelServletPath());
			assertEquals("SQRL Java Server Demo", config.getServerFriendlyName());
			final byte[] expectedKeyBytes = new byte[] { 40, 34, 52, 4, -78, 19, 113, -104, -111, -15, -65, 33, -89,
					117, 86, -76 };
			ArrayAssert.assertEquals(expectedKeyBytes, config.getAESKeyBytes());
		}
	}
}
