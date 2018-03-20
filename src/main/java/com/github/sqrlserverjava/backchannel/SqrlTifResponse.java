package com.github.sqrlserverjava.backchannel;

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
	private final int tifInt;

	private SqrlTifResponse(final int tifInt) {
		this.tifInt = tifInt;
	}

	public String toHexString() {
		return Integer.toHexString(tifInt).toUpperCase();
	}

	@Override
	public String toString() {
		return toHexString();
	}

	public static class SqrlTifResponseBuilder {
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
			return new SqrlTifResponse(builderTifInt);
		}
	}
}
