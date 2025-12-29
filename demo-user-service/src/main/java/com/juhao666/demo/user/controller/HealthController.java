package com.juhao666.demo.user.controller;

import com.juhao666.demo.user.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RefreshScope
@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${registry.instance.service-name}")
    private String serviceName;
    @Value("${registry.instance.instance-id}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String applicationName;

    @GetMapping("/health")
    public Result health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("applicationName", applicationName);
        data.put("service", serviceName);
        data.put("instanceId", instanceId);
        data.put("timestamp", System.currentTimeMillis());
        return Response.success("服务健康", data);
    }
}
