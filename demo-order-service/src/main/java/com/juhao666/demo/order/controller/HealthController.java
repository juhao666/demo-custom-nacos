package com.juhao666.demo.order.controller;

import com.juhao666.demo.order.model.Response;
import com.juhao666.demo.order.model.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${registry.instance.service-name}")
    private String serviceName;
    @Value("${registry.instance.instance-id}")
    private String instanceId;

    @GetMapping("/health")
    public Result health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", serviceName);
        data.put("instanceId", instanceId);
        data.put("timestamp", System.currentTimeMillis());
        return Response.success("服务健康", data);
    }
}
