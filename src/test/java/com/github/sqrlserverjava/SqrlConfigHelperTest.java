package com.github.sqrlserverjava;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Test;

import com.github.sqrlserverjava.enums.SqrlQrCodeImageFormat;
import com.github.sqrlserverjava.exception.SqrlIllegalStateException;
import com.github.sqrlserverjava.util.SqrlConfigHelper;
import com.github.sqrlserverjava.util.SqrlConstants;

import junitx.framework.ObjectAssert;
import junitx.framework.StringAssert;

public class SqrlConfigHelperTest {

	@Test
	public void testLoadOk() throws Throwable {
		final SqrlConfig sqrlConfig = SqrlConfigHelper.loadFromClasspath();
		assertNotNull(sqrlConfig);
		assertEquals("sqrlbc", sqrlConfig.getBackchannelServletPath());
		final byte[] expectedAesKeyBytes = Base64.getDecoder().decode("DhMncY4ErDcLRfwfyeN02Q==".getBytes(SqrlConstants.UTF8_CHARSET)); 
		assertEquals("DhMncY4ErDcLRfwfyeN02Q==", sqrlConfig.getAesKeyBase64());
		// Verify defaults are in effect since they were not defined in XML
		assertNull(sqrlConfig.getClientAuthStateUpdaterClass());  // Will be null at first, set later
		assertEquals(900, sqrlConfig.getNutValidityInSeconds());
		assertEquals(SqrlQrCodeImageFormat.PNG, sqrlConfig.getQrCodeImageFormat());
		assertNull(sqrlConfig.getIpForwardedForHeader());
		assertEquals(1, sqrlConfig.getIpForwardedForHeaderList().size());
		assertEquals("X-Forwarded-For", sqrlConfig.getIpForwardedForHeaderList().get(0));
		assertEquals("com.github.sqrlserverjava.persistence.SqrlJpaPersistenceFactory", sqrlConfig.getSqrlPersistenceFactoryClass());
		assertEquals(15, sqrlConfig.getCleanupTaskExecInMinutes());
		assertEquals(500, sqrlConfig.getAuthSyncCheckInMillis());
		assertEquals("/sqrllogin", sqrlConfig.getSqrlLoginServletPath());
		assertEquals(true, sqrlConfig.isEnableCps());
		assertEquals("sqrlcorrelator", sqrlConfig.getCorrelatorCookieName());
		assertEquals("sqrlfirstnut", sqrlConfig.getFirstNutCookieName());
		assertNull(sqrlConfig.getCookieDomain()); // Will be null at first, set later
		assertEquals("/", sqrlConfig.getCookiePath());
	}

	// TODO: test all default values are loaded

	@Test
	public void testLoadAll() throws Throwable {
		final SqrlConfig sqrlConfig = SqrlConfigHelper.loadFromClasspath("sqrlConfigAll.xml");
		assertNotNull(sqrlConfig);

		// Check data		
		assertEquals("sqrlbc", sqrlConfig.getBackchannelServletPath());
		final byte[] expectedAesKeyBytes = Base64.getDecoder().decode("oYqoDiWZiODUW2eJ5y8dNA==".getBytes(SqrlConstants.UTF8_CHARSET)); 
		assertEquals("oYqoDiWZiODUW2eJ5y8dNA==", sqrlConfig.getAesKeyBase64());
		assertEquals("com.me.MyClientAuthStateUpdaterClass", sqrlConfig.getClientAuthStateUpdaterClass());
		assertEquals(300, sqrlConfig.getNutValidityInSeconds());
		assertEquals(SqrlQrCodeImageFormat.PNG, sqrlConfig.getQrCodeImageFormat());
		assertEquals(Arrays.toString(new String[] {"one"}), Arrays.toString(sqrlConfig.getIpForwardedForHeader()));
		assertEquals(Collections.singletonList("one"), sqrlConfig.getIpForwardedForHeaderList());
		assertEquals("com.me.MySqrlPersistenceFactoryClass", sqrlConfig.getSqrlPersistenceFactoryClass());
		assertEquals(9, sqrlConfig.getCleanupTaskExecInMinutes());
		assertEquals(200, sqrlConfig.getAuthSyncCheckInMillis());
		assertEquals("/customPath", sqrlConfig.getSqrlLoginServletPath());
		assertEquals(false, sqrlConfig.isEnableCps());
		assertEquals("cc", sqrlConfig.getCorrelatorCookieName());
		assertEquals("sfnc", sqrlConfig.getFirstNutCookieName());
		assertEquals("sauth.me.com", sqrlConfig.getCookieDomain());
		assertEquals("/sauth", sqrlConfig.getCookiePath());
	}

	@Test
	public void testLoadMultipleXForwardedForHeaders() throws Throwable {
		final SqrlConfig sqrlConfig = SqrlConfigHelper.loadFromClasspath("sqrlConfigHeaders.xml");
		assertNotNull(sqrlConfig);
		assertEquals(2, sqrlConfig.getIpForwardedForHeader().length);
		assertEquals(Arrays.toString(new String[] {"one", "two"}), Arrays.toString(sqrlConfig.getIpForwardedForHeader()));

		assertEquals(2, sqrlConfig.getIpForwardedForHeaderList().size());
		assertEquals(Arrays.asList(new String[] {"one", "two"}), sqrlConfig.getIpForwardedForHeaderList());
	}

	@Test
	public void testNotFound() throws Throwable {
		try {
			SqrlConfigHelper.loadFromClasspath("somefile.xml");
			fail("Exception expected");
		} catch (final Exception e) {
			ObjectAssert.assertInstanceOf(IllegalStateException.class, e);
			StringAssert.assertContains("not found", e.getMessage());
		}
	}

	@Test
	public void testSomeOtherXml() throws Throwable {
		try {
			SqrlConfigHelper.loadFromClasspath("other.xml");
			fail("Exception expected");
		} catch (final Exception e) {
			ObjectAssert.assertInstanceOf(IllegalStateException.class, e);
			StringAssert.assertContains("Error unmarshalling", e.getMessage());
		}
	}

	@Test
	public void testInvalidXml() throws Exception {
		try {
			SqrlConfigHelper.loadFromClasspath("sqrlconfigInvalid.xml");
			fail("Exception expected");
		} catch (final SqrlIllegalStateException e) {
			StringAssert.assertContains("schema validation failed", e.getMessage());
		}
	}

	/* ************** non-test ****************/

	public static void main(final String[] args) {
		final SqrlConfig sqrlConfig = new SqrlConfig();
		sqrlConfig.setBackchannelServletPath("sqrlbc");
		final SecureRandom secureRandom = new SecureRandom();
		final byte[] aesKeyBytes = new byte[16];
		secureRandom.nextBytes(aesKeyBytes);
		sqrlConfig.setAesKeyBase64(TestCaseUtil.AES_TEST_KEY);
		sqrlConfig.setClientAuthStateUpdaterClass("com.me.MyClientAuthStateUpdaterClass");
		sqrlConfig.setNutValidityInSeconds(200);
		sqrlConfig.setQrCodeImageFormat(SqrlQrCodeImageFormat.JPG);
		sqrlConfig.setIpForwardedForHeader(new String[] {"one", "two"});
		sqrlConfig.setSqrlPersistenceFactoryClass("com.me.MySqrlPersistenceFactoryClass");
		sqrlConfig.setCleanupTaskExecInMinutes(9);
		sqrlConfig.setAuthSyncCheckInMillis(200);
		sqrlConfig.setSqrlLoginServletPath("/customPath");
		sqrlConfig.setEnableCps(false);
		sqrlConfig.setCorrelatorCookieName("cc");
		sqrlConfig.setFirstNutCookieName("sfnc");
		sqrlConfig.setCookieDomain("sauth.me.com");
		sqrlConfig.setCookiePath("/sauth");
		try {
			final JAXBContext jaxbContext = JAXBContext.newInstance(SqrlConfig.class);
			final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(sqrlConfig, System.out);

		} catch (final JAXBException e) {
			e.printStackTrace();
		}
	}

}
