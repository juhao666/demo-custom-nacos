package com.juhao666.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
//备用类，包含所有服务组件
//@SpringBootApplication
//@EnableScheduling
//@RestController
//@RequestMapping("/api/v1")
public class RegistryCenterApplication {

    // 服务注册表：serviceName -> List<ServiceInstance>
    private static final Map<String, List<ServiceInstance>> SERVICE_REGISTRY =
            new ConcurrentHashMap<>();

    // 配置存储：dataId -> ConfigItem
    private static final Map<String, ConfigItem> CONFIG_STORE =
            new ConcurrentHashMap<>();

    // 心跳时间戳：instanceId -> lastHeartbeatTime
    private static final Map<String, Long> HEARTBEAT_TIMESTAMPS =
            new ConcurrentHashMap<>();

    // 配置监听器
    private static final Map<String, List<DeferredResult<ConfigItem>>> CONFIG_LISTENERS =
            new ConcurrentHashMap<>();

    // 服务实例ID生成器
    private static final AtomicLong INSTANCE_ID_GENERATOR = new AtomicLong(1);

    // 健康检查阈值（30秒）
    private static final long HEALTH_CHECK_THRESHOLD = 30000;

    // 心跳线程池
    private static final ScheduledExecutorService HEARTBEAT_EXECUTOR =
            Executors.newScheduledThreadPool(2);

    public static void main(String[] args) {
        SpringApplication.run(RegistryCenterApplication.class, args);
        System.out.println("==========================================");
        System.out.println("微服务注册中心启动成功！");
        System.out.println("端口: 8848");
        System.out.println("API文档:");
        System.out.println("  - 服务注册: POST /api/v1/instance/register");
        System.out.println("  - 服务注销: POST /api/v1/instance/deregister");
        System.out.println("  - 心跳上报: POST /api/v1/instance/heartbeat");
        System.out.println("  - 服务发现: GET /api/v1/instance/list?serviceName={name}");
        System.out.println("  - 获取配置: GET /api/v1/config?dataId={id}&group={group}");
        System.out.println("  - 发布配置: POST /api/v1/config");
        System.out.println("  - 监听配置: GET /api/v1/config/listener");
        System.out.println("  - 查看所有服务: GET /api/v1/instance/all");
        System.out.println("==========================================");

        // 启动健康检查定时任务
        startHealthCheckTask();
    }

    /**
     * 服务注册接口
     */
    @PostMapping("/instance/register")
    public Result registerInstance(@RequestBody ServiceInstance instance) {
        try {
            // 参数校验
            validateInstance(instance);

            // 生成实例ID
            String instanceId = generateInstanceId(instance);
            instance.setInstanceId(instanceId);
            instance.setRegistrationTime(System.currentTimeMillis());
            instance.setLastHeartbeatTime(System.currentTimeMillis());
            instance.setStatus("UP");

            // 添加到注册表
            String serviceName = instance.getServiceName();
            SERVICE_REGISTRY.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>());
            List<ServiceInstance> instances = SERVICE_REGISTRY.get(serviceName);

            // 检查是否已注册
            Optional<ServiceInstance> existing = instances.stream()
                    .filter(i -> i.getInstanceId().equals(instanceId))
                    .findFirst();

            if (!existing.isPresent()) {
                instances.add(instance);
                HEARTBEAT_TIMESTAMPS.put(instanceId, System.currentTimeMillis());
                System.out.println("✅ 服务注册成功: " + serviceName + " [" + instanceId + "]");
            } else {
                // 更新心跳时间
                HEARTBEAT_TIMESTAMPS.put(instanceId, System.currentTimeMillis());
                existing.get().setLastHeartbeatTime(System.currentTimeMillis());
                System.out.println("🔄 服务心跳更新: " + serviceName + " [" + instanceId + "]");
            }

            return Result.success("服务注册成功", instance);
        } catch (Exception e) {
            return Result.error("服务注册失败: " + e.getMessage());
        }
    }

    /**
     * 服务注销接口
     */
    @PostMapping("/instance/deregister")
    public Result deregisterInstance(@RequestParam String serviceName,
                                     @RequestParam String instanceId) {
        try {
            if (SERVICE_REGISTRY.containsKey(serviceName)) {
                List<ServiceInstance> instances = SERVICE_REGISTRY.get(serviceName);
                boolean removed = instances.removeIf(instance ->
                        instance.getInstanceId().equals(instanceId));

                if (removed) {
                    HEARTBEAT_TIMESTAMPS.remove(instanceId);
                    System.out.println("❌ 服务注销成功: " + serviceName + " [" + instanceId + "]");
                }

                // 如果服务没有实例了，移除服务
                if (instances.isEmpty()) {
                    SERVICE_REGISTRY.remove(serviceName);
                }
            }
            return Result.success("服务注销成功");
        } catch (Exception e) {
            return Result.error("服务注销失败: " + e.getMessage());
        }
    }

    /**
     * 心跳上报接口
     */
    @PostMapping("/instance/heartbeat")
    public Result heartbeat(@RequestParam String serviceName,
                            @RequestParam String instanceId) {
        try {
            HEARTBEAT_TIMESTAMPS.put(instanceId, System.currentTimeMillis());

            // 更新实例的最后心跳时间
            List<ServiceInstance> instances = SERVICE_REGISTRY.get(serviceName);
            if (instances != null) {
                for (ServiceInstance instance : instances) {
                    if (instance.getInstanceId().equals(instanceId)) {
                        instance.setLastHeartbeatTime(System.currentTimeMillis());
                        break;
                    }
                }
            }

            return Result.success("心跳上报成功", System.currentTimeMillis());
        } catch (Exception e) {
            return Result.error("心跳上报失败: " + e.getMessage());
        }
    }

    /**
     * 服务发现接口
     */
    @GetMapping("/instance/list")
    public Result discoverServices(@RequestParam String serviceName) {
        try {
            List<ServiceInstance> instances = SERVICE_REGISTRY.getOrDefault(serviceName,
                    new ArrayList<>());

            // 过滤掉不健康的实例
            List<ServiceInstance> healthyInstances = new ArrayList<>();
            for (ServiceInstance instance : instances) {
                Long lastHeartbeat = HEARTBEAT_TIMESTAMPS.get(instance.getInstanceId());
                if (lastHeartbeat != null &&
                        (System.currentTimeMillis() - lastHeartbeat) < HEALTH_CHECK_THRESHOLD) {
                    instance.setStatus("UP");
                    healthyInstances.add(instance);
                } else {
                    instance.setStatus("DOWN");
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("serviceName", serviceName);
            data.put("instances", healthyInstances);
            data.put("total", healthyInstances.size());
            data.put("timestamp", System.currentTimeMillis());

            return Result.success("服务发现成功", data);
        } catch (Exception e) {
            return Result.error("服务发现失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有服务
     */
    @GetMapping("/instance/all")
    public Result getAllServices() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("totalServices", SERVICE_REGISTRY.size());

            Map<String, List<ServiceInstance>> healthyServices = new HashMap<>();
            for (Map.Entry<String, List<ServiceInstance>> entry : SERVICE_REGISTRY.entrySet()) {
                String serviceName = entry.getKey();
                List<ServiceInstance> healthyInstances = new ArrayList<>();

                for (ServiceInstance instance : entry.getValue()) {
                    Long lastHeartbeat = HEARTBEAT_TIMESTAMPS.get(instance.getInstanceId());
                    if (lastHeartbeat != null &&
                            (System.currentTimeMillis() - lastHeartbeat) < HEALTH_CHECK_THRESHOLD) {
                        instance.setStatus("UP");
                        healthyInstances.add(instance);
                    }
                }

                if (!healthyInstances.isEmpty()) {
                    healthyServices.put(serviceName, healthyInstances);
                }
            }

            data.put("services", healthyServices);
            data.put("timestamp", System.currentTimeMillis());

            return Result.success("获取所有服务成功", data);
        } catch (Exception e) {
            return Result.error("获取所有服务失败: " + e.getMessage());
        }
    }

    /**
     * 获取配置接口
     */
    @GetMapping("/config")
    public Result getConfig(@RequestParam String dataId,
                            @RequestParam(required = false, defaultValue = "DEFAULT_GROUP") String group) {
        try {
            String key = generateConfigKey(dataId, group);
            ConfigItem config = CONFIG_STORE.get(key);

            if (config == null) {
                return Result.error("配置不存在");
            }

            return Result.success("获取配置成功", config);
        } catch (Exception e) {
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 发布配置接口
     */
    @PostMapping("/config")
    public Result publishConfig(@RequestBody ConfigItem config) {
        try {
            // 参数校验
            if (config.getDataId() == null || config.getContent() == null) {
                return Result.error("dataId和content不能为空");
            }

            if (config.getGroup() == null) {
                config.setGroup("DEFAULT_GROUP");
            }

            // 生成版本号和MD5
            config.setVersion(config.getVersion() == 0 ? 1 : config.getVersion() + 1);
            config.setMd5(calculateMD5(config.getContent()));
            config.setUpdateTime(System.currentTimeMillis());

            // 存储配置
            String key = generateConfigKey(config.getDataId(), config.getGroup());
            CONFIG_STORE.put(key, config);

            System.out.println("📝 配置发布成功: " + key + " v" + config.getVersion());

            // 通知监听器
            notifyConfigListeners(key, config);

            return Result.success("配置发布成功", config);
        } catch (Exception e) {
            return Result.error("配置发布失败: " + e.getMessage());
        }
    }

    /**
     * 配置监听接口（长轮询）
     */
    @GetMapping("/config/listener")
    public DeferredResult<Result> listenConfig(@RequestParam String dataId,
                                               @RequestParam(required = false, defaultValue = "DEFAULT_GROUP") String group,
                                               @RequestParam(required = false) String md5) {
        String key = generateConfigKey(dataId, group);
        ConfigItem currentConfig = CONFIG_STORE.get(key);

        DeferredResult<Result> deferredResult = new DeferredResult<>(30000L);
        deferredResult.onTimeout(() -> {
            deferredResult.setResult(Result.success("监听超时"));
        });

        // 检查配置是否变更
        if (currentConfig != null && md5 != null && !currentConfig.getMd5().equals(md5)) {
            deferredResult.setResult(Result.success("配置已变更", currentConfig));
        } else {
            // 添加到监听列表
            CONFIG_LISTENERS.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
           // CONFIG_LISTENERS.get(key).add(deferredResult);

            // 设置完成回调
            deferredResult.onCompletion(() -> {
                List<DeferredResult<ConfigItem>> listeners = CONFIG_LISTENERS.get(key);
                if (listeners != null) {
                    listeners.remove(deferredResult);
                }
            });
        }

        return deferredResult;
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        data.put("serviceCount", SERVICE_REGISTRY.size());
        data.put("configCount", CONFIG_STORE.size());
        return Result.success("服务健康", data);
    }

    // ==================== 私有方法 ====================

    private void validateInstance(ServiceInstance instance) {
        if (instance.getServiceName() == null) {
            throw new IllegalArgumentException("serviceName不能为空");
        }
        if (instance.getIp() == null) {
            throw new IllegalArgumentException("ip不能为空");
        }
        if (instance.getPort() <= 0) {
            throw new IllegalArgumentException("port必须大于0");
        }
    }

    private String generateInstanceId(ServiceInstance instance) {
        if (instance.getInstanceId() != null) {
            return instance.getInstanceId();
        }
        return instance.getServiceName() + "-" + instance.getIp() + ":" +
                instance.getPort() + "-" + INSTANCE_ID_GENERATOR.getAndIncrement();
    }

    private String generateConfigKey(String dataId, String group) {
        return dataId + ":" + group;
    }

    private String calculateMD5(String content) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void notifyConfigListeners(String key, ConfigItem config) {
        List<DeferredResult<ConfigItem>> listeners = CONFIG_LISTENERS.get(key);
        if (listeners != null && !listeners.isEmpty()) {
            for (DeferredResult<ConfigItem> listener : listeners) {
                listener.setResult(config);
            }
            listeners.clear();
        }
    }

    private static void startHealthCheckTask() {
        HEARTBEAT_EXECUTOR.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            int removedCount = 0;

            for (Map.Entry<String, List<ServiceInstance>> entry : SERVICE_REGISTRY.entrySet()) {
                String serviceName = entry.getKey();
                List<ServiceInstance> instances = entry.getValue();

                Iterator<ServiceInstance> iterator = instances.iterator();
                while (iterator.hasNext()) {
                    ServiceInstance instance = iterator.next();
                    String instanceId = instance.getInstanceId();
                    Long lastHeartbeat = HEARTBEAT_TIMESTAMPS.get(instanceId);

                    if (lastHeartbeat == null ||
                            (currentTime - lastHeartbeat) > HEALTH_CHECK_THRESHOLD) {
                        // 实例不健康，移除
                        iterator.remove();
                        HEARTBEAT_TIMESTAMPS.remove(instanceId);
                        removedCount++;
                        System.out.println("⚠️ 移除不健康实例: " + serviceName + " [" + instanceId + "]");
                    }
                }

                // 如果服务没有实例了，移除服务
                if (instances.isEmpty()) {
                    SERVICE_REGISTRY.remove(serviceName);
                }
            }

            if (removedCount > 0) {
                System.out.println("健康检查完成，移除 " + removedCount + " 个不健康实例");
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    // ==================== 数据模型 ====================

    public static class ServiceInstance {
        private String serviceName;      // 服务名
        private String instanceId;       // 实例ID
        private String ip;               // IP地址
        private int port;                // 端口
        private String status;           // 状态：UP, DOWN
        private Map<String, String> metadata; // 元数据
        private long registrationTime;   // 注册时间
        private long lastHeartbeatTime;  // 最后心跳时间

        public ServiceInstance() {
            this.metadata = new HashMap<>();
        }

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

        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

        public long getRegistrationTime() { return registrationTime; }
        public void setRegistrationTime(long registrationTime) { this.registrationTime = registrationTime; }

        public long getLastHeartbeatTime() { return lastHeartbeatTime; }
        public void setLastHeartbeatTime(long lastHeartbeatTime) { this.lastHeartbeatTime = lastHeartbeatTime; }
    }

    public static class ConfigItem {
        private String dataId;           // 配置ID
        private String group;            // 分组
        private String content;          // 配置内容
        private String type;             // 类型：properties, yaml, json, xml
        private long version;            // 版本号
        private String md5;              // 内容MD5
        private long updateTime;         // 更新时间

        public String getDataId() { return dataId; }
        public void setDataId(String dataId) { this.dataId = dataId; }

        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public long getVersion() { return version; }
        public void setVersion(long version) { this.version = version; }

        public String getMd5() { return md5; }
        public void setMd5(String md5) { this.md5 = md5; }

        public long getUpdateTime() { return updateTime; }
        public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
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
