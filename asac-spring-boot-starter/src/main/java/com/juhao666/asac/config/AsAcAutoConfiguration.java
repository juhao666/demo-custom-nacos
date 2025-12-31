package com.juhao666.asac.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.juhao666.asac.client.ConfigListener;
import com.juhao666.asac.client.RegistrationService;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties(AsAcProperties.class)
public class AsAcAutoConfiguration implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
        scanner.scan("com.juhao666.asac");
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }
    @Bean
    @ConditionalOnMissingBean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(1);
    }

    @Bean
    @ConditionalOnMissingBean
    public RegistrationService registrationService(
            AsAcProperties properties,
            RestTemplate restTemplate,
            ScheduledExecutorService executorService) {
        return new RegistrationService(properties, restTemplate, executorService);
    }

    @Bean
    @ConditionalOnMissingBean(value = ConfigListener.class, name = "configListener")
    public ConfigListener configListener(
            AsAcProperties properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            ConfigurableEnvironment environment,
            ContextRefresher contextRefresher) {
        return new ConfigListener(properties, restTemplate, objectMapper, environment, contextRefresher);
    }
}
