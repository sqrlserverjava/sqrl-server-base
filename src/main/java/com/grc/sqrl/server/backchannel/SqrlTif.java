package com.grc.sqrl.server.backchannel;

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

	private final int tifInt;

	private SqrlTif(final int tifInt) {
		this.tifInt = tifInt;
	}

	public byte[] getTifBytes() {
		return SqrlNutTokenUtil.unpack(tifInt);
	}

	public int getTifInt() {
		return tifInt;
	}

	public static class TifBuilder {
		private byte builderTifByte;

		public TifBuilder(final boolean ipsMatched) {
			if (ipsMatched) {
				setFlag(TIF_IPS_MATCHED);
			}
		}

		public TifBuilder setFlag(final int tifFlag) {
			builderTifByte |= tifFlag;
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
