package com.github.dbadia.sqrl.server.backchannel;

// @formatter:off
/**
 * Valid values for the SQRL tif indicator, see https://www.grc.com/sqrl/semantics.htm
 *
 * This library uses these flags in the following manner, which is understood to be compliant with the SQRL spec
 *
 * Server side errors:   	COMMAND_FAILED
 * Invalid client request:  COMMAND_FAILED & CLIENT_FAILURE
 *
 * @author Dave Badia
 *
 */
//@formatter:on
public class SqrlTif {


	private final int tifInt;

	private SqrlTif(final int tifInt) {
		this.tifInt = tifInt;
	}

	public String toHexString() {
		return Integer.toHexString(tifInt).toUpperCase();
	}

	@Override
	public String toString() {
		return toHexString();
	}

	public static class SqrlTifBuilder {
		private int builderTifInt;

		public SqrlTifBuilder(final boolean ipsMatched) {
			if (ipsMatched) {
				addFlag(SqrlTifFlag.IPS_MATCHED);
			}
		}

		public SqrlTifBuilder() {
		}

		public SqrlTifBuilder addFlag(SqrlTifFlag tifFlag) {
			builderTifInt |= tifFlag.getMask();
			return this;
		}

		/**
		 * Removes all flags that are set
		 */
		public SqrlTifBuilder clearAllFlags() {
			builderTifInt = 0;
			return this;
		}

		public SqrlTif createTif() {
			return new SqrlTif(builderTifInt);
		}
	}
}
