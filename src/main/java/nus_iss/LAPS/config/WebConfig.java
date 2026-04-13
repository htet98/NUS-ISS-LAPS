package nus_iss.LAPS.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration.
 * Enables static resources from /static and configures the view layer.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {



    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Convenience: /home redirects to /
        registry.addRedirectViewController("/home", "/");
    }
}
