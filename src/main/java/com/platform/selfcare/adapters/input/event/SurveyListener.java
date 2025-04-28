package com.platform.selfcare.adapters.input.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.platform.selfcare.domain.event.SurveyCreatedEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SurveyListener {
	
	@EventListener
	public void handle(final SurveyCreatedEvent event) {
		log.info(String.format("New survey triggered by themeId: %s at %s message "
				+ "exists: %s and identification left: %s", 
				event.getThemeId(), event.getDate(), event.getMessageExists(), event.getIdentifierExists()));
	}
}
