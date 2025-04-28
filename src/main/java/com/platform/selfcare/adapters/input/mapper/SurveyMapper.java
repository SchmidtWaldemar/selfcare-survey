package com.platform.selfcare.adapters.input.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.platform.selfcare.adapters.input.request.SurveyRequest;
import com.platform.selfcare.adapters.input.request.ThemeType;
import com.platform.selfcare.adapters.output.entity.SurveyEntity;
import com.platform.selfcare.domain.model.Survey;
import com.platform.selfcare.domain.exception.ThemeNotFoundException;
import com.platform.selfcare.domain.model.Answer;
import com.platform.selfcare.domain.model.ChoiseType;
import com.platform.selfcare.domain.model.Question;

@Service
public class SurveyMapper {
	
	public Survey toSurvey(SurveyEntity entity) {
		List<Question> questions = createMinimalQuestionList(entity.getTheme());
		return Survey.builder()
				.identifier(entity.getEmail())
				.questions(questions)
				.message(entity.getMessage())
				.themeId(entity.getTheme().getId())
				.build();
	}

	public SurveyEntity toSurveyEntity(Survey survey) {
		return SurveyEntity.builder()
				.email(survey.getIdentifier())
				.message(survey.getMessage())
				.theme(
					findThemeById(survey.getThemeId())
				)
				.build();
	}
	
	public Survey toSurvey(SurveyRequest request) {
		List<Question> questions = createMinimalQuestionList(request.type());
		return Survey.builder()
				.message(request.message())
				.questions(questions)
				.identifier(request.email())
				.themeId(request.type().getId())
				.build();
	}
	
	public ThemeType findThemeById(Integer themeId) {
		return Arrays.stream(ThemeType.values())
			.filter(t -> t.getId() == themeId)
			.findAny()
			.orElseThrow(() -> new ThemeNotFoundException(String.format("Not existing Theme by id: %s", themeId)));
	}
	
	/**
	 * provisoric question fulfilling
	 * 
	 * @param themeType
	 * @return
	 */
	private List<Question> createMinimalQuestionList(ThemeType themeType) {
		List<Answer> answers = new ArrayList<Answer>();
		answers.add(
			Answer.builder()
				.answer(themeType.getTheme())
				.build()
		);
		List<Question> questions = new ArrayList<Question>();
		questions.add(
			Question.builder()
				.answers(answers)
				.question(themeType.getTheme())
				.type(ChoiseType.SINGLECHOISE).build()
		);
		return questions;
	}
	
}
