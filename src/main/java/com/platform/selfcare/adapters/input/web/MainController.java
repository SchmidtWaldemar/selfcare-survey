package com.platform.selfcare.adapters.input.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.platform.selfcare.adapters.input.mapper.SurveyMapper;
import com.platform.selfcare.adapters.input.request.SurveyRequest;
import com.platform.selfcare.adapters.input.request.ThemeType;
import com.platform.selfcare.adapters.input.response.SurveyResult;
import com.platform.selfcare.application.port.input.CreateSurveyUseCase;
import com.platform.selfcare.application.port.input.GetSurveyUseCase;
import com.platform.selfcare.domain.model.Survey;

import jakarta.validation.Valid;

@Controller
public class MainController {
	
	@Autowired
	private CreateSurveyUseCase createSurveyUseCase;
	
	@Autowired
	private GetSurveyUseCase getSurveyUseCase;
	
	@Autowired
	private SurveyMapper surveyMapper;

	@GetMapping("/")
	public String index() {
		return "index";
	}
	
	@ModelAttribute
	public void addAttributes(Model model) {
		model.addAttribute("themes", ThemeType.values());
	}
	
	@GetMapping(path="/surveyForm")
	public String survey(Model model) {
		if (model.getAttribute("survey") == null) {
			model.addAttribute("survey", new SurveyRequest(null, null, null));
		}
		return "surveyForm";
	}
	
	@PostMapping(path="/surveyForm/add")
	public String addSurvey(@Valid @ModelAttribute("survey") SurveyRequest requestDto, 
			BindingResult result, RedirectAttributes redirectAttr, Model model)  {
		
		if (result.hasErrors()) {
			redirectAttr.addFlashAttribute("survey", requestDto);
			return "surveyForm";
		}
		
		Survey survey = this.surveyMapper.toSurvey(requestDto);
		
		survey = this.createSurveyUseCase.createSurvey(survey);
		
		return result(model);
	}

	@GetMapping(path="/surveyResult")
	public String result(Model model) {
		List<SurveyResult> results = new ArrayList<SurveyResult>();
		for (ThemeType theme : ThemeType.values()) {
			Long count = getSurveyUseCase.getCountByThemeId(theme.getId());
			results.add(new SurveyResult(count, theme));
		}
		model.addAttribute("results", results);
		return "surveyResult";
	}
}
