package com.juhao666.asac.config;

import com.juhao666.asac.service.RegistrationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ComponentScan("com.juhao666.asac")
@EnableConfigurationProperties(AsAcProperties.class)
@ConditionalOnProperty(prefix = "asac", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsAcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public ScheduledExecutorService heartbeatExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistrationService registrationService(
            AsAcProperties properties,
            RestTemplate restTemplate,
            ScheduledExecutorService executorService) {
        return new RegistrationService(properties, restTemplate, executorService);
    }
}
