package com.platform.selfcare.domain.model;

import java.util.List;

import jakarta.validation.constraints.NotNull;
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
public class Question {
	
	private String question;
	
	private List<Answer> answers;
	
	@NotNull
	private ChoiseType type;
}
