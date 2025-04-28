package com.platform.selfcare.adapters.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.platform.selfcare.adapters.input.mapper.SurveyMapper;
import com.platform.selfcare.adapters.output.SurveyPersistenceAdapter;
import com.platform.selfcare.adapters.output.event.SurveyPublisher;
import com.platform.selfcare.adapters.output.repository.SurveyRepository;
import com.platform.selfcare.application.port.service.SurveyEventService;

@Configuration
public class BeanConfiguration {
	
	@Bean
	SurveyPersistenceAdapter surveyPersistenceAdapter(final SurveyRepository surveyRepository, final SurveyMapper surveyMapper) {
		return new SurveyPersistenceAdapter(surveyRepository, surveyMapper);
	}
	
	@Bean
	SurveyPublisher surveyPublisher(final ApplicationEventPublisher applicationEventPublisher) {
		return new SurveyPublisher(applicationEventPublisher);
	}
	
	@Bean
	SurveyEventService surveyEventService(final SurveyPersistenceAdapter surveyPersistenceAdapter, final SurveyPublisher surveyPublisher) {
		return new SurveyEventService(surveyPublisher, surveyPersistenceAdapter);
	}
}
