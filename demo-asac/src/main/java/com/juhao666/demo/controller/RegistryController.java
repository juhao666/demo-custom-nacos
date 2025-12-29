package com.juhao666.demo.controller;

import com.juhao666.demo.model.Result;
import com.juhao666.demo.model.Response;
import com.juhao666.demo.model.ServiceInstance;
import com.juhao666.demo.store.RegistryStore;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 服务注册接口
 */

@RestController
@RequestMapping("/api/v1")
public class RegistryController {

    @Autowired
    RegistryStore registryStore;
    // 健康检查阈值（30秒）
    private static final long HEALTH_CHECK_THRESHOLD = 30000;

    @PostMapping("/instance/register")
    public Result registerInstance(@RequestBody @Valid ServiceInstance instance) {
        try {
            ServiceInstance registeredInstance = registryStore.registerInstance(instance);
            return Response.success("服务注册成功", registeredInstance);
        } catch (Exception e) {
            return Response.error("服务注册失败: " + e.getMessage());
        }
    }

    /**
     * 服务注销接口
     */
    @PostMapping("/instance/deregister")
    public Result deregisterInstance(@RequestParam String serviceName, @RequestParam String instanceId) {
        try {
             boolean success = registryStore.deregisterInstance(serviceName,instanceId);
             if (success) {
                 return Response.success("服务注销成功");
             }
             return Response.error("服务注销失败");
        } catch (Exception e) {
            return Response.error("服务注销失败: " + e.getMessage());
        }
    }

    /**
     * 心跳上报接口
     */
    @PostMapping("/instance/heartbeat")
    public Result heartbeat(@RequestParam String serviceName, @RequestParam String instanceId) {

        try {
            registryStore.updateHeartbeat(instanceId);
            //System.out.printf("心跳上报成功. Service Name = %s, Instance Id = %s%n", serviceName, instanceId);
            return Response.success("心跳上报成功", System.currentTimeMillis());
        } catch (Exception e) {
            return Response.error("心跳上报失败: " + e.getMessage());
        }
    }

    private boolean isInstanceHealthy(ServiceInstance instance) {
        Long lastHeartbeat = registryStore.getHeartbeatTime(instance.getInstanceId());
        if (lastHeartbeat == null) {
            return false;
        }
        return (System.currentTimeMillis() - lastHeartbeat) < HEALTH_CHECK_THRESHOLD;
    }
    /**
     * 服务发现接口
     */
    @GetMapping("/instance/list")
    public Result discoverServices(@RequestParam String serviceName) {
        try {
            List<ServiceInstance> allInstances = registryStore.getInstances(serviceName);

            // 过滤掉不健康的实例
            List<ServiceInstance> healthyInstances = allInstances.stream()
                    .filter(instance -> isInstanceHealthy(instance))
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("serviceName", serviceName);
            data.put("instances", healthyInstances);
            data.put("total", healthyInstances.size());
            data.put("timestamp", System.currentTimeMillis());

            return Response.success("服务发现成功", data);
        } catch (Exception e) {
            return Response.error("服务发现失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有服务
     */
    @GetMapping("/instance/all")
    public Result getAllServices() {
        try {
            Map<String, List<ServiceInstance>> allServices = registryStore.getAllServices();
            Map<String, List<ServiceInstance>> healthyServices = new HashMap<>();
            for (Map.Entry<String, List<ServiceInstance>> entry : allServices.entrySet()) {
                List<ServiceInstance> healthyInstances = entry.getValue().stream()
                        .filter(instance -> isInstanceHealthy(instance))
                        .collect(Collectors.toList());

                if (!healthyInstances.isEmpty()) {
                    healthyServices.put(entry.getKey(), healthyInstances);
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("totalServices", healthyServices.size());
            data.put("services", healthyServices);
            data.put("timestamp", System.currentTimeMillis());

            return Response.success("获取所有服务成功", data);
        } catch (Exception e) {
            return Response.error("获取所有服务失败: " + e.getMessage());
        }
    }
}
