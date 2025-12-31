package com.juhao666.demo.product.service;

import com.juhao666.asac.model.Response;
import com.juhao666.asac.model.Result;
import com.juhao666.asac.model.ServiceInstance;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DiscoverService {

    private static final String REGISTRY_URL = "http://localhost:8848/api/v1";

    // 服务发现缓存
    private final Map<String, List<ServiceInstance>> serviceCache = new ConcurrentHashMap<>();


    public Result findActiveService() {
        // 从注册中心发现用户服务
        RestTemplate restTemplate = new RestTemplate();
        try {
            Result result = restTemplate.getForObject(
                    REGISTRY_URL + "/instance/list?serviceName=user-service",
                    Result.class
            );

            if (result != null && result.isSuccess()) {
                Map<String, Object> data = (Map<String, Object>) result.getData();
                if (data != null) {
                    List<Map<String, Object>> instancesData = (List<Map<String, Object>>) data.get("instances");
                    List<ServiceInstance> instances = new ArrayList<>();

                    for (Map<String, Object> instanceData : instancesData) {
                        ServiceInstance instance = new ServiceInstance();
                        instance.setServiceName((String) instanceData.get("serviceName"));
                        instance.setInstanceId((String) instanceData.get("instanceId"));
                        instance.setIp((String) instanceData.get("ip"));
                        if (instanceData.get("port") instanceof Integer) {
                            instance.setPort((Integer) instanceData.get("port"));
                        }
                        instances.add(instance);
                    }

                    serviceCache.put("user-service", instances);
                    return Response.success("发现用户服务成功", instances);
                }
            }
            return Response.error("用户服务不可用");
        } catch (Exception e) {
            return Response.error("发现用户服务失败: " + e.getMessage());
        }
    }
}
