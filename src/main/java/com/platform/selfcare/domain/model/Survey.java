package com.platform.selfcare.domain.model;

import java.util.List;

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
public class Survey {

	/**
	 * main theme of survey
	 */
	private Integer themeId;
	
	/**
	 * can be email of participant
	 */
	private String identifier;
	
	/**
	 * optional message of participant
	 */
	private String message;
	
	private List<Question> questions;
}
