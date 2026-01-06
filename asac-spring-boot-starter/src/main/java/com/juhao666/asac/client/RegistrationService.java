package com.juhao666.asac.client;

import com.juhao666.asac.config.AsAcProperties;
import com.juhao666.asac.model.Result;
import com.juhao666.asac.model.ServiceInstance;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RegistrationService {
    private final AsAcProperties properties;
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService heartbeatExecutor;
    private ScheduledFuture<?> heartbeatFuture;

    public RegistrationService(AsAcProperties properties,
                               RestTemplate restTemplate,
                               ScheduledExecutorService executorService) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.heartbeatExecutor = executorService;
    }

    /**
     * 它会在 Spring Boot 应用程序完全启动并准备就绪时执行。具体来说：
     * 1.执行时机 ：
     * 在 Spring 容器完全初始化之后
     * 所有的 Bean 都已经创建完成
     * 所有的依赖注入都已经完成
     * 所有的初始化回调（如 @PostConstruct）都已执行完毕
     * 内嵌的 web 服务器（如果有）已经启动
     * 应用程序完全可以对外提供服务时
     * 2.Spring Boot 启动顺序 ：
     * → 应用启动
     * → Bean 实例化
     * → 依赖注入
     * → @PostConstruct
     * → ApplicationStartedEvent
     * → ApplicationReadyEvent（这个方法在这里执行）
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerToRegistry() {
        log.info("Registering to service registry...");
        try {
            Result result = restTemplate.postForObject(
                    properties.getRegistryUrl() + "/instance/register",
                    createServiceInstance(),
                    Result.class
            );

            if (result != null && result.isSuccess()) {
                log.info("Successfully registered to service registry");
                startHeartbeatTask();
            } else {
                log.error("Registration failed: {}",
                        result != null ? result.getMessage() : "Unknown error");
            }
        } catch (Exception e) {
            log.error("Failed to register to service registry", e);
        }
    }

    private void startHeartbeatTask() {
        heartbeatFuture = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                restTemplate.postForObject(
                        properties.getRegistryUrl() + "/instance/heartbeat",
                        createServiceInstance(),
                        Result.class
                );
            } catch (Exception e) {
                log.error("Failed to send heartbeat", e);
            }
        }, 0, properties.getHeartbeatInterval(), TimeUnit.MILLISECONDS);
    }

    private ServiceInstance createServiceInstance() {
        ServiceInstance instance = new ServiceInstance();
        instance.setServiceName(properties.getServiceName());
        //product-service--localhost:8002
        instance.setInstanceId(properties.getServiceName() + "-" +
                properties.getIp() + ":" + properties.getPort());
        instance.setIp(properties.getIp());
        instance.setPort(properties.getPort());
        instance.setStatus("UP");
        return instance;
    }

    @PreDestroy
    public void destroy() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
        heartbeatExecutor.shutdown();
    }
}

