package com.platform.selfcare.application.port.output;

import com.platform.selfcare.domain.event.SurveyCreatedEvent;

public interface SurveyEventPublisher {
	void publishSurveyCreatedEvent(SurveyCreatedEvent event);
}
