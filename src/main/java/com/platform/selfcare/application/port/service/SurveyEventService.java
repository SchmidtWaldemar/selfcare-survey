package com.platform.selfcare.application.port.service;

import java.util.List;

import com.platform.selfcare.application.port.input.CreateSurveyUseCase;
import com.platform.selfcare.application.port.input.GetSurveyUseCase;
import com.platform.selfcare.application.port.output.SurveyEventPublisher;
import com.platform.selfcare.application.port.output.SurveyOutputPort;
import com.platform.selfcare.domain.event.SurveyCreatedEvent;
import com.platform.selfcare.domain.model.Survey;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SurveyEventService implements CreateSurveyUseCase, GetSurveyUseCase {
	
	private final SurveyEventPublisher surveyEventPublisher;
	private final SurveyOutputPort surveyOutputPort;
	
	@Override
	public Survey createSurvey(Survey survey) {
		survey = this.surveyOutputPort.saveSurvey(survey);
		
		this.surveyEventPublisher.publishSurveyCreatedEvent(
				new SurveyCreatedEvent(
					survey.getThemeId(), 
					survey.getIdentifier() != null && survey.getIdentifier().length() > 1, 
					!survey.getMessage().isBlank()
				)
		);
		
		return survey;
	}

	@Override
	public List<Survey> findAllSurveys() {
		return this.surveyOutputPort.findAllSurveys();
	}

	@Override
	public Long getCountByThemeId(Integer id) {
		return this.surveyOutputPort.getCountByTheme(id);
	}
}
