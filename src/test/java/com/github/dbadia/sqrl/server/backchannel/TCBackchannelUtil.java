package com.github.dbadia.sqrl.server.backchannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

public class TCBackchannelUtil {

	public static SqrlClientRequest buildMockSqrlRequest(final String idk, final String command,
			final boolean hasUrsSignature, final SqrlClientOpt... optArray) {
		final SqrlClientRequest mock = Mockito.mock(SqrlClientRequest.class);
		Mockito.when(mock.containsUrs()).thenReturn(false);
		Mockito.when(mock.getIdk()).thenReturn(idk);
		Mockito.when(mock.getClientCommand()).thenReturn(command);
		Mockito.when(mock.containsUrs()).thenReturn(hasUrsSignature);
		final List<SqrlClientOpt> optList = new ArrayList<>();
		optList.addAll(Arrays.asList(optArray));
		Mockito.when(mock.getOptList()).thenReturn(optList);
		return mock;
	}
}
