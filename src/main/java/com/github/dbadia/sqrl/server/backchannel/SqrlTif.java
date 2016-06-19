package com.github.dbadia.sqrl.server.backchannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Valid values for the SQRL tif indicator
 * 
 * @author Dave Badia
 *
 */
public class SqrlTif {
	public static final int TIF_CURRENT_ID_MATCH = 0x01;
	public static final int TIF_PREVIOUS_ID_MATCH = 0x02;
	public static final int TIF_IPS_MATCHED = 0x04;
	public static final int TIF_SQRL_DISABLED = 0x08;
	public static final int TIF_FUNCTIONS_NOT_SUPPORTED = 0x10;
	public static final int TIF_TRANSIENT_ERROR = 0x20;
	public static final int TIF_COMMAND_FAILED = 0x40;
	public static final int TIF_CLIENT_FAILURE = 0x80;
	public static final int TIF_BAD_ID_ASSOCIATION = 0x100;

	private static final List<Integer> ALL_TIF_LIST = new ArrayList<>();

	static {
		ALL_TIF_LIST.add(TIF_CURRENT_ID_MATCH);
		ALL_TIF_LIST.add(TIF_PREVIOUS_ID_MATCH);
		ALL_TIF_LIST.add(TIF_IPS_MATCHED);
		ALL_TIF_LIST.add(TIF_SQRL_DISABLED);
		ALL_TIF_LIST.add(TIF_FUNCTIONS_NOT_SUPPORTED);
		ALL_TIF_LIST.add(TIF_TRANSIENT_ERROR);
		ALL_TIF_LIST.add(TIF_COMMAND_FAILED);
		ALL_TIF_LIST.add(TIF_CLIENT_FAILURE);
		ALL_TIF_LIST.add(TIF_BAD_ID_ASSOCIATION);
	}

	private final int tifInt;

	private SqrlTif(final int tifInt) {
		this.tifInt = tifInt;
	}

	static final List<Integer> getAllTifs() {
		return new ArrayList(ALL_TIF_LIST);
	}

	public int getTifInt() {
		return tifInt;
	}

	public static class TifBuilder {
		private byte builderTifByte;

		public TifBuilder(final boolean ipsMatched) {
			if (ipsMatched) {
				addFlag(TIF_IPS_MATCHED);
			}
		}

		public TifBuilder() {
		}

		public TifBuilder addFlag(final int tifFlag) {
			builderTifByte |= tifFlag;
			return this;
		}

		/**
		 * Removes all flags that are set
		 */
		public TifBuilder clearAllFlags() {
			builderTifByte = 0;
			return this;
		}

		public SqrlTif createTif() {
			return new SqrlTif(builderTifByte);
		}
	}

	@Override
	public String toString() {
		return Integer.toString(tifInt);
	}
}
