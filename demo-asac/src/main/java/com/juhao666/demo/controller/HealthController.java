package com.juhao666.demo.controller;

import com.juhao666.demo.model.Response;
import com.juhao666.demo.model.Result;
import com.juhao666.demo.store.RegistryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    @Autowired
    RegistryStore registryStore;
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result health() {
        Map<String, Object> data = registryStore.getStatistics();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        return Response.success("服务健康", data);
    }
}
