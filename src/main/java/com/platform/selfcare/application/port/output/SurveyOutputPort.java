package com.platform.selfcare.application.port.output;

import java.util.List;

import com.platform.selfcare.domain.model.Survey;

public interface SurveyOutputPort {
	Survey saveSurvey(Survey survey);
	List<Survey> findAllSurveys();
	Long getCountByTheme(Integer themeId);
}
