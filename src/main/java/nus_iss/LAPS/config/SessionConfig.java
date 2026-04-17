package nus_iss.LAPS.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

@Configuration
@EnableSpringHttpSession
public class SessionConfig {

    @Bean
    public MapSessionRepository sessionRepository() {
        MapSessionRepository repo = new MapSessionRepository(new ConcurrentHashMap<>());
        repo.setDefaultMaxInactiveInterval(Duration.ofMinutes(30));// 30 minutes
        return repo;
    }
}
