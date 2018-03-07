package com.github.sqrlserverjava;

import java.util.Collections;

import javax.persistence.Persistence;

public class GenrerateJpaSchemas {
	public static void main(String[] args) {
		try {
			Persistence.generateSchema("javasqrl-persistence", Collections.EMPTY_MAP);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
