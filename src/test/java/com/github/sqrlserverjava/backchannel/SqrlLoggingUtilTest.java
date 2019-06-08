package com.github.sqrlserverjava.backchannel;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.sqrlserverjava.backchannel.LoggingUtil.LogField;

public class SqrlLoggingUtilTest {
	@Test
	public void testAllFieldsLogged() {
		final List<LogField> fullFieldList = new ArrayList<>(Arrays.asList(LogField.values()));

		LoggingUtil.HEADER_FIELD_ORDER.stream().forEach(f -> fullFieldList.remove(f));
		LoggingUtil.FOOTER_FIELD_ORDER.stream().forEach(f -> fullFieldList.remove(f));

		assertTrue(
				"Need to add the following to SqrlClientRequestLoggingUtil.HEADER_FIELD_ORDER or FOOTER_FIELD_ORDER: "
						+ fullFieldList,
				fullFieldList.isEmpty());
	}
}
