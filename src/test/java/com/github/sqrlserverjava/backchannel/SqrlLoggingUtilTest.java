package com.github.sqrlserverjava.backchannel;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.sqrlserverjava.backchannel.SqrlClientRequestLoggingUtil.LogField;

public class SqrlLoggingUtilTest {
	@Test
	public void testAllFieldsLogged() {
		final List<LogField> fullFieldList = new ArrayList<>(Arrays.asList(LogField.values()));

		SqrlClientRequestLoggingUtil.HEADER_FIELD_ORDER.stream().forEach(f -> fullFieldList.remove(f));
		SqrlClientRequestLoggingUtil.FOOTER_FIELD_ORDER.stream().forEach(f -> fullFieldList.remove(f));

		assertTrue("Fields not being logged: " + fullFieldList, fullFieldList.isEmpty());
	}
}
