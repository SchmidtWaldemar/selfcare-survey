package com.platform.selfcare.adapters.input.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ThemeType {
	ANGST(1, "Angst"),
	WUT(2, "Wut"),
	SCHMERZEN(3, "Schmerzen"),
	TRAUMA(4, "Trauma");

	private int id;
	private String theme;
}
