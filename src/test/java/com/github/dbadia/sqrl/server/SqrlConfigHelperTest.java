package com.github.dbadia.sqrl.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.SecureRandom;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Test;

import com.github.dbadia.sqrl.server.util.SqrlConfigHelper;

import junitx.framework.ObjectAssert;
import junitx.framework.StringAssert;

public class SqrlConfigHelperTest {

	@Test
	public void testLoadOk() throws Throwable {
		final SqrlConfig sqrlConfig = SqrlConfigHelper.loadFromClasspath();
		assertNotNull(sqrlConfig);
		assertEquals("sqrlbc", sqrlConfig.getBackchannelServletPath());
		// Test defaults
		assertEquals("sqrlcorrelator", sqrlConfig.getCorrelatorCookieName());
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

	/* ************** non-test ****************/

	public static void main(final String[] args) {
		final SqrlConfig sqrlConfig = new SqrlConfig();
		sqrlConfig.setBackchannelServletPath("sqrlbc");
		final SecureRandom secureRandom = new SecureRandom();
		final byte[] aesKeyBytes = new byte[16];
		secureRandom.nextBytes(aesKeyBytes);
		sqrlConfig.setAESKeyBytes(aesKeyBytes);
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
