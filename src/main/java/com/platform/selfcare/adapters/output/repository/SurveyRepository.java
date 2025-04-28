package com.platform.selfcare.adapters.output.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.platform.selfcare.adapters.input.request.ThemeType;
import com.platform.selfcare.adapters.output.entity.SurveyEntity;

public interface SurveyRepository extends JpaRepository<SurveyEntity, Long> {
	
	long countByTheme(ThemeType type);
}
