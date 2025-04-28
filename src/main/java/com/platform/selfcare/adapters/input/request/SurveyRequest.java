package com.platform.selfcare.adapters.input.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record SurveyRequest(
		@Email(message ="E-Mail should have correct format")
		String email,
		
		String message,
		
		@NotNull(message = "Type of Theme should be selected")
		ThemeType type
	) {
}
