package com.platform.selfcare.application.port.input;

import java.util.List;

import com.platform.selfcare.domain.model.Survey;

public interface GetSurveyUseCase {
	List<Survey> findAllSurveys();
	Long getCountByThemeId(Integer id);
}
