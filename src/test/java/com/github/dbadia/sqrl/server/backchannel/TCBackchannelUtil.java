package com.github.dbadia.sqrl.server.backchannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

import com.github.dbadia.sqrl.server.util.SqrlServerSideKey;

public class TCBackchannelUtil {

	public static SqrlClientRequest buildMockSqrlRequest(final String idk, final SqrlRequestCommand command,
			final String correlator,
			final boolean hasUrsSignature, final SqrlRequestOpt... optArray) {
		final SqrlClientRequest mock = Mockito.mock(SqrlClientRequest.class);
		Mockito.when(mock.containsUrs()).thenReturn(false);
		Mockito.when(mock.getKey(SqrlServerSideKey.idk)).thenReturn(idk);
		Mockito.when(mock.getCorrelator()).thenReturn(correlator);
		Mockito.when(mock.getClientCommand()).thenReturn(command);
		Mockito.when(mock.containsUrs()).thenReturn(hasUrsSignature);
		final List<SqrlRequestOpt> optList = new ArrayList<>();
		optList.addAll(Arrays.asList(optArray));
		Mockito.when(mock.getOptList()).thenReturn(optList);
		return mock;
	}
}
