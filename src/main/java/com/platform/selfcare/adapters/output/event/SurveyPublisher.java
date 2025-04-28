package com.platform.selfcare.adapters.output.event;

import org.springframework.context.ApplicationEventPublisher;

import com.platform.selfcare.application.port.output.SurveyEventPublisher;
import com.platform.selfcare.domain.event.SurveyCreatedEvent;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SurveyPublisher implements SurveyEventPublisher {
	private final ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void publishSurveyCreatedEvent(SurveyCreatedEvent event) {
		this.applicationEventPublisher.publishEvent(event);
	}
}
