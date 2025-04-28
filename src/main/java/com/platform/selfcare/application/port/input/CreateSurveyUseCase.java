package com.platform.selfcare.application.port.input;

import com.platform.selfcare.domain.model.Survey;

public interface CreateSurveyUseCase {
	Survey createSurvey(Survey survey);
}
