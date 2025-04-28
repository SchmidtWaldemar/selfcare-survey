package com.platform.selfcare.domain.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SurveyCreatedEvent {
	private Integer themeId;
	private Boolean identifierExists;
	private Boolean messageExists;
	private LocalDateTime date;
	
	public SurveyCreatedEvent(Integer themeId, boolean idExists, boolean msgExists) {
		this.themeId = themeId;
		this.identifierExists = idExists;
		this.messageExists = msgExists;
		this.date = LocalDateTime.now();
	}
}
