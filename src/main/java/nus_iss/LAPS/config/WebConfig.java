package nus_iss.LAPS.config;

import nus_iss.LAPS.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.FormatterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for Spring MVC.
 * Registers custom formatters for model binding.
 */
@Component
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new LeaveTypeFormatter(leaveTypeRepository));
    }
}

