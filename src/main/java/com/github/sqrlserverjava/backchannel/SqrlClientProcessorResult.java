package com.github.sqrlserverjava.backchannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sqrlserverjava.enums.SqrlInternalUserState;

public class SqrlClientProcessorResult {
	private static final Logger logger = LoggerFactory.getLogger(SqrlClientProcessorResult.class);

	private SqrlInternalUserState sqrlInternalUserState = SqrlInternalUserState.NONE_EXIST;
	private final Map<String, String> replyDataTable = new ConcurrentHashMap<>();


	void addDataToReply(final String name, final String value) {
		final String oldValue = replyDataTable.get(name);
		if (oldValue != null) {
			logger.warn("replyDataTable already contained {}={}   Updating value to {}", name, oldValue, value);
		}
		replyDataTable.put(name, value);
	}

	public SqrlInternalUserState getSqrlInternalUserState() {
		return sqrlInternalUserState;
	}

	public void setSqrlInternalUserState(final SqrlInternalUserState sqrlInternalUserState) {
		this.sqrlInternalUserState = sqrlInternalUserState;
	}

	public Map<String, String> getReplyDataTable() {
		return replyDataTable;
	}
}
