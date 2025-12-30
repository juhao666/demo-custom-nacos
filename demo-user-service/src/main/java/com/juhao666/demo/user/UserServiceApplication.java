package com.juhao666.demo.user;

import com.juhao666.demo.user.listener.ConfigListener;
import com.juhao666.demo.user.model.Result;
import com.juhao666.demo.user.model.User;
import com.juhao666.demo.user.model.ServiceInstance;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
public class UserServiceApplication {

    // 注册中心地址
    private static final String REGISTRY_URL = "http://localhost:8848/api/v1";

    // 当前服务信息
    private static final String SERVICE_NAME = "user-service";
    private static final String INSTANCE_ID = "user-service-localhost:8001"; //order-service-localhost:8003-3
    private static final int PORT = 8001;

    //todo 支持配置变更坚挺的代码
    @Bean
    public ConfigListener configListener() {
        return new ConfigListener();
    }


    // 心跳线程池
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor();

    // 服务发现线程池
    private final ScheduledExecutorService discoveryExecutor =
            Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("==========================================");
        System.out.println("用户服务启动成功！");
        System.out.println("端口: " + PORT);
        System.out.println("API文档:");
        System.out.println("  - 获取所有用户: GET /api/users");
        System.out.println("  - 根据ID获取用户: GET /api/users/{id}");
        System.out.println("  - 创建用户: POST /api/users");
        System.out.println("  - 发现商品服务: GET /api/discover/product-service");
        System.out.println("  - 发现订单服务: GET /api/discover/order-service");
        System.out.println("  - 健康检查: GET /api/health");
        System.out.println("==========================================");
    }



    /**
     * @EventListener是 Spring 框架中用于实现事件驱动编程的核心注解，其核心作用是将普通方法标记为事件监听器， 在特定事件发布时自动触发方法执行。
     *
     * 解耦事件发布与处理
     * 发布者（Publisher）：通过 ApplicationEventPublisher发布事件。
     * 监听者（Listener）：通过 @EventListener标记的方法接收事件，无需直接依赖发布者。
     */

    @EventListener(ApplicationReadyEvent.class)
    public void registerToRegistry() {
        System.out.println("正在注册到注册中心...");

        RestTemplate restTemplate = new RestTemplate();

        try {
            Result result = restTemplate.postForObject(
                    REGISTRY_URL + "/instance/register",
                    instance(),
                    Result.class
            );

            if (result != null && result.isSuccess()) {
                System.out.println("✅ 成功注册到注册中心");

                // 启动心跳线程
                startHeartbeatTask();

                // 启动服务发现线程 todo  should in asac??
                //startDiscoveryTask();
            } else {
                System.err.println("❌ 注册失败: " + (result != null ? result.getMessage() : "未知错误"));
            }
        } catch (Exception e) {
            System.err.println("❌ 注册到注册中心失败: " + e.getMessage());
        }
    }

    /**
     * 启动心跳任务
     */
    private void startHeartbeatTask() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            RestTemplate restTemplate = new RestTemplate();
            try {
                restTemplate.postForObject(
                        REGISTRY_URL + "/instance/heartbeat",
                        instance(),
                        Result.class
                );
            } catch (Exception e) {
                System.err.println("心跳发送失败: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * 启动服务发现任务
     */
    private void startDiscoveryTask() {
        discoveryExecutor.scheduleAtFixedRate(() -> {
            // 发现商品服务
            discoverService("product-service");

            // 发现订单服务
            //discoverService("order-service");
        }, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * 服务发现
     */
    private void discoverService(String serviceName) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            Result result = restTemplate.getForObject(
                    REGISTRY_URL + "/instance/list?serviceName=" + serviceName,
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
                    //todo 先注释调，整个服务发下那应该是server端需要处理的。
                    //serviceCache.put(serviceName, instances);
                    System.out.println("🔍 发现服务 [" + serviceName + "]，可用实例数: " + instances.size());
                }
            }
        } catch (Exception e) {
            // 服务发现失败，不打印错误日志避免刷屏
        }
    }

    private ServiceInstance instance() {
        ServiceInstance instance = new ServiceInstance();
        instance.setServiceName(SERVICE_NAME);
        instance.setInstanceId(INSTANCE_ID);
        instance.setIp("localhost");
        instance.setPort(PORT);
        instance.setStatus("UP");
        return instance;
    }


}
