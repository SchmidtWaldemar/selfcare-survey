package com.platform.selfcare.adapters.input.response;

import com.platform.selfcare.adapters.input.request.ThemeType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SurveyResult(
		@Min(0)
		Long count,
		
		@NotNull(message = "Type of Theme should be selected")
		ThemeType type
	) {
}
