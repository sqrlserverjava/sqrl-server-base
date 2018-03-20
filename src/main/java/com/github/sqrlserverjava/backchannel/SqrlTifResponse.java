package com.github.sqrlserverjava.backchannel;

import java.util.HashSet;
import java.util.Set;

// @formatter:off
/**
 * Represents the SQRL tif response value, containing zero or more
 * <code>SqrlTifFlag</code>, that will be transmitted to the SQRL client.
 * 
 * @see SqrlTifFlag
 * @author Dave Badia
 *
 */
//@formatter:on
public class SqrlTifResponse {
	private final Set<SqrlTifFlag> tifFlagList;
	private final int tifInt;

	private SqrlTifResponse(Set<SqrlTifFlag> tifFlagList, final int tifInt) {
		this.tifFlagList = tifFlagList;
		this.tifInt = tifInt;
	}

	public String toHexString() {
		return Integer.toHexString(tifInt).toUpperCase();
	}

	@Override
	public String toString() {
		return new StringBuilder(100).append(toHexString()).append(tifFlagList).toString();
	}

	public static class SqrlTifResponseBuilder {
		private Set<SqrlTifFlag> tifFlagList = new HashSet<>();
		private int builderTifInt;

		public SqrlTifResponseBuilder(final boolean ipsMatched) {
			if (ipsMatched) {
				addFlag(SqrlTifFlag.IPS_MATCHED);
			}
		}

		public SqrlTifResponseBuilder() {
		}

		public SqrlTifResponseBuilder addFlag(SqrlTifFlag tifFlag) {
			builderTifInt |= tifFlag.getMask();
			tifFlagList.add(tifFlag);
			return this;
		}

		/**
		 * Removes all flags that are set
		 */
		public SqrlTifResponseBuilder clearAllFlags() {
			builderTifInt = 0;
			return this;
		}

		public SqrlTifResponse createTif() {
			return new SqrlTifResponse(tifFlagList, builderTifInt);
		}
	}
}
