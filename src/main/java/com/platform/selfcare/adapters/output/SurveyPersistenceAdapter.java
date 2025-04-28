package com.platform.selfcare.adapters.output;

import java.util.List;

import com.platform.selfcare.adapters.input.mapper.SurveyMapper;
import com.platform.selfcare.adapters.input.request.ThemeType;
import com.platform.selfcare.adapters.output.entity.SurveyEntity;
import com.platform.selfcare.adapters.output.repository.SurveyRepository;
import com.platform.selfcare.application.port.output.SurveyOutputPort;
import com.platform.selfcare.domain.model.Survey;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SurveyPersistenceAdapter implements SurveyOutputPort {
	
	private final SurveyRepository surveyRepository;
	
	private final SurveyMapper surveyMapper;

	@Override
	public Survey saveSurvey(Survey survey) {
		SurveyEntity surveyEntity = this.surveyMapper.toSurveyEntity(survey);
		surveyEntity = this.surveyRepository.save(surveyEntity);
		return this.surveyMapper.toSurvey(surveyEntity);
	}

	@Override
	public List<Survey> findAllSurveys() {
		List<SurveyEntity> surveyEntities = this.surveyRepository.findAll();
		List<Survey> surveys = surveyEntities.stream().map(s -> this.surveyMapper.toSurvey(s)).toList();
		return surveys;
	}

	@Override
	public Long getCountByTheme(Integer themeId) {
		ThemeType theme =  this.surveyMapper.findThemeById(themeId);
		return surveyRepository.countByTheme(theme);
	}
}