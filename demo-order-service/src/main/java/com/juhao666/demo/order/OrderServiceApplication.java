package com.juhao666.demo.order;

import com.juhao666.demo.order.model.Result;
import com.juhao666.demo.order.model.ServiceInstance;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
public class OrderServiceApplication {

    // 注册中心地址
    private static final String REGISTRY_URL = "http://localhost:8848/api/v1";

    // 当前服务信息
    private static final String SERVICE_NAME = "order-service";
    private static final String INSTANCE_ID = "order-service-001";
    private static final int PORT = 8003;


    // 心跳线程池
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("==========================================");
        System.out.println("商品服务启动成功！");
        System.out.println("端口: " + PORT);
        System.out.println("API文档:");
        System.out.println("  - 获取所有商品: GET /api/orders");
        System.out.println("  - 根据ID获取商品: GET /api/orders/{id}");
        System.out.println("  - 创建商品: POST /api/orders");
        System.out.println("  - 发现用户服务: GET /api/discover/user-service");
        System.out.println("  - 获取商品详情（带用户信息）: GET /api/orders/{id}/detail");
        System.out.println("==========================================");
    }



    @EventListener(ApplicationReadyEvent.class)
    public void registerToRegistry() {
        System.out.println("正在注册到注册中心...");

        RestTemplate restTemplate = new RestTemplate();
        ServiceInstance instance = new ServiceInstance();
        instance.setServiceName(SERVICE_NAME);
        instance.setInstanceId(INSTANCE_ID);
        instance.setIp("localhost");
        instance.setPort(PORT);
        instance.setStatus("UP");

        try {
            Result result = restTemplate.postForObject(
                    REGISTRY_URL + "/instance/register",
                    instance,
                    Result.class
            );

            if (result != null && result.isSuccess()) {
                System.out.println("✅ 成功注册到注册中心");
                startHeartbeatTask();
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
                        REGISTRY_URL + "/instance/heartbeat?serviceName=" + SERVICE_NAME +
                                "&instanceId=" + INSTANCE_ID,
                        null,
                        Result.class
                );
            } catch (Exception e) {
                System.err.println("心跳发送失败: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }



}

