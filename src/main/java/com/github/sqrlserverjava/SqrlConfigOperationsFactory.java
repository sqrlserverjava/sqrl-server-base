package com.github.sqrlserverjava;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SqrlConfigOperationsFactory {
	private static Map<SqrlConfig, WeakReference<SqrlConfigOperations>> cacheTable = new ConcurrentHashMap<>();
	
	private SqrlConfigOperationsFactory() {
		// factory 
	}
	
	public static synchronized SqrlConfigOperations get(SqrlConfig config) {
		SqrlConfigOperations operations = null;
		WeakReference<SqrlConfigOperations> weakOperationsRef = cacheTable.get(config);
		if(weakOperationsRef != null) {
			operations = weakOperationsRef.get();
		}
		if(operations == null) {
			operations = new SqrlConfigOperations(config);
			cacheTable.put(config, new WeakReference<>(operations));
		}
		return operations;
	}

}
