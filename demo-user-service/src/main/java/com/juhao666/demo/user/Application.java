package com.juhao666.demo.user;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//备用类，包含所有服务组件
//@SpringBootApplication
//@EnableScheduling
//@RestController
//@RequestMapping("/api")
public class Application {

    // 注册中心地址
    private static final String REGISTRY_URL = "http://localhost:8848/api/v1";

    // 当前服务信息
    private static final String SERVICE_NAME = "user-service";
    private static final String INSTANCE_ID = "user-service-001";
    private static final int PORT = 8001;

    // 服务发现缓存
    private final Map<String, List<ServiceInstance>> serviceCache = new ConcurrentHashMap<>();

    // 内存存储用户数据
    private final Map<Long, User> userDatabase = new ConcurrentHashMap<>();

    // 心跳线程池
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor();

    // 服务发现线程池
    private final ScheduledExecutorService discoveryExecutor =
            Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("==========================================");
        System.out.println("用户服务启动成功！");
        System.out.println("端口: " + PORT);
        System.out.println("API文档:");
        System.out.println("  - 获取所有用户: GET /api/users");
        System.out.println("  - 根据ID获取用户: GET /api/users/{id}");
        System.out.println("  - 创建用户: POST /api/users");
        System.out.println("  - 发现商品服务: GET /api/discover/product-service");
        System.out.println("  - 发现订单服务: GET /api/discover/order-service");
        System.out.println("==========================================");
    }

    @PostConstruct
    public void initData() {
        // 初始化测试数据
        userDatabase.put(1L, new User(1L, "张三", "zhangsan@example.com", "13800138001"));
        userDatabase.put(2L, new User(2L, "李四", "lisi@example.com", "13800138002"));
        userDatabase.put(3L, new User(3L, "王五", "wangwu@example.com", "13800138003"));
        System.out.println("✅ 初始化测试数据完成，共 " + userDatabase.size() + " 个用户");
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

                    serviceCache.put(serviceName, instances);
                    System.out.println("🔍 发现服务 [" + serviceName + "]，可用实例数: " + instances.size());
                }
            }
        } catch (Exception e) {
            // 服务发现失败，不打印错误日志避免刷屏
        }
    }

    // ==================== API接口 ====================

    @GetMapping("/users")
    public Result getAllUsers() {
        List<User> users = new ArrayList<>(userDatabase.values());
        return Result.success("获取用户列表成功", users);
    }

    @GetMapping("/users/{id}")
    public Result getUserById(@PathVariable Long id) {
        User user = userDatabase.get(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success("获取用户成功", user);
    }

    @PostMapping("/users")
    public Result createUser(@RequestBody User user) {
        Long newId = userDatabase.keySet().stream()
                .max(Long::compareTo)
                .orElse(0L) + 1;
        user.setId(newId);
        userDatabase.put(newId, user);

        return Result.success("创建用户成功", user);
    }

    @GetMapping("/discover/product-service")
    public Result discoverProductService() {
        List<ServiceInstance> instances = serviceCache.get("product-service");
        if (instances == null || instances.isEmpty()) {
            return Result.error("未发现商品服务实例");
        }
        return Result.success("发现商品服务成功", instances);
    }

    @GetMapping("/discover/order-service")
    public Result discoverOrderService() {
        List<ServiceInstance> instances = serviceCache.get("order-service");
        if (instances == null || instances.isEmpty()) {
            return Result.error("未发现订单服务实例");
        }
        return Result.success("发现订单服务成功", instances);
    }

    @GetMapping("/health")
    public Result health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", SERVICE_NAME);
        data.put("instanceId", INSTANCE_ID);
        data.put("timestamp", System.currentTimeMillis());
        return Result.success("服务健康", data);
    }

    // ==================== 数据模型 ====================

    public static class User {
        private Long id;
        private String name;
        private String email;
        private String phone;

        public User() {}

        public User(Long id, String name, String email, String phone) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone = phone;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class ServiceInstance {
        private String serviceName;
        private String instanceId;
        private String ip;
        private int port;
        private String status;

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }

        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class Result {
        private boolean success;
        private String message;
        private Object data;
        private long timestamp;

        public Result(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public static Result success(String message) {
            return new Result(true, message, null);
        }

        public static Result success(String message, Object data) {
            return new Result(true, message, data);
        }

        public static Result error(String message) {
            return new Result(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
