package com.platform.selfcare.domain.exception;

public class ThemeNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -269583250958536407L;

	public ThemeNotFoundException(String message) {
		super(message);
	}
}
