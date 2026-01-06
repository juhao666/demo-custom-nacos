package com.juhao666.demo.store;

import com.juhao666.demo.listener.ConfigListener;
import com.juhao666.demo.model.ConfigItem;
import com.juhao666.demo.model.ServiceInstance;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/**
 * 注册中心存储组件 - 线程安全设计
 * 使用ConcurrentHashMap和StampedLock保证高并发读写性能
 */
@Component
public class RegistryStore {

    // 核心存储数据结构 - 使用ConcurrentHashMap保证线程安全
    //单例Bean + final 确保变量全局唯一。
    private final Map<String, List<ServiceInstance>> serviceRegistry = new ConcurrentHashMap<>();
    private final Map<String, ConfigItem> configStore = new ConcurrentHashMap<>();

    // 心跳时间戳缓存 - 使用ConcurrentHashMap
    private final Map<String, Long> heartbeatTimestamps = new ConcurrentHashMap<>();

    // 配置监听器 - 使用线程安全的CopyOnWriteArrayList
    private final Map<String, List<ConfigListener>> configListeners = new ConcurrentHashMap<>();

    // 读写锁用于更复杂的操作
    private final ReadWriteLock configLock = new ReentrantReadWriteLock();
    private final StampedLock serviceLock = new StampedLock();

    // 实例ID生成器 - 使用AtomicLong保证原子性
    private final AtomicLong instanceIdGenerator = new AtomicLong(1);

    // 统计信息
    private final Map<String, Object> statistics = new ConcurrentHashMap<>();



    @PostConstruct
    public void init() {
        ConfigItem orderServiceConfig = new ConfigItem();
        orderServiceConfig.setDataId("demo-order-service.properties");
        orderServiceConfig.setGroup("DEFAULT_GROUP");
        orderServiceConfig.setContent("order.name=order-config-1");
        orderServiceConfig.setMd5(calculateMD5(orderServiceConfig.getContent()));
        configStore.put("demo-order-service.properties:DEFAULT_GROUP", orderServiceConfig);

        System.out.println("✅ RegistryStore初始化完成，使用线程安全存储结构");
        statistics.put("startTime", System.currentTimeMillis());
        statistics.put("serviceCount", 0L);
        statistics.put("configCount", 0L);
        statistics.put("totalOperations", 0L);
    }

    @PreDestroy
    public void destroy() {
        System.out.println("🛑 RegistryStore正在关闭，清理资源...");
        clearAll();
    }

    // ==================== 服务实例操作 ====================

    /**
     * 注册服务实例 - 线程安全
     */
    public ServiceInstance registerInstance(ServiceInstance instance) {
        if (instance == null || instance.getServiceName() == null) {
            throw new IllegalArgumentException("服务实例参数无效");
        }

        String serviceName = instance.getServiceName();
        String instanceId = generateInstanceId(instance);
        instance.setInstanceId(instanceId);
        instance.setRegistrationTime(System.currentTimeMillis());
        instance.setLastHeartbeatTime(System.currentTimeMillis());
        instance.setStatus("UP");

        long stamp = serviceLock.writeLock();
        try {
            // 获取或创建服务实例列表
            List<ServiceInstance> instances = serviceRegistry.computeIfAbsent(
                    serviceName,
                    k -> Collections.synchronizedList(new ArrayList<>())
            );

            // 检查是否已存在
            Optional<ServiceInstance> existing = instances.stream()
                    .filter(i -> i.getInstanceId().equals(instanceId))
                    .findFirst();

            if (existing.isPresent()) {
                // 更新现有实例
                ServiceInstance existingInstance = existing.get();
                existingInstance.setLastHeartbeatTime(System.currentTimeMillis());
                existingInstance.setStatus("UP");
                existingInstance.setMetadata(instance.getMetadata());
                heartbeatTimestamps.put(instanceId, System.currentTimeMillis());

                serviceRegistry.put(serviceName, instances);

                updateStatistics("update");
                System.out.println("🔄 服务心跳更新: " + serviceName + " [" + instanceId + "]");
                return existingInstance;
            } else {
                // 添加新实例
                instance.setRegistrationTime(System.currentTimeMillis());
                instance.setLastHeartbeatTime(System.currentTimeMillis());
                instance.setStatus("UP");

                instances.add(instance);
                heartbeatTimestamps.put(instanceId, System.currentTimeMillis());

                updateStatistics("register");
                updateServiceCount(serviceName, instances.size());
                System.out.println("✅ 服务注册成功: " + serviceName + " [" + instanceId + "]");
                return instance;
            }
        } finally {
            serviceLock.unlockWrite(stamp);
        }
    }

    /**
     * 注销服务实例 - 线程安全
     */
    public boolean deregisterInstance(String serviceName, String instanceId) {
        if (serviceName == null || instanceId == null) {
            return false;
        }

        long stamp = serviceLock.writeLock();
        try {
            List<ServiceInstance> instances = serviceRegistry.get(serviceName);
            if (instances != null) {
                boolean removed = instances.removeIf(instance ->
                        instance.getInstanceId().equals(instanceId));

                if (removed) {
                    heartbeatTimestamps.remove(instanceId);

                    // 如果服务没有实例了，移除服务
                    if (instances.isEmpty()) {
                        serviceRegistry.remove(serviceName);
                        updateServiceCount(serviceName, 0);
                    } else {
                        updateServiceCount(serviceName, instances.size());
                    }

                    updateStatistics("deregister");
                    return true;
                }
            }
            return false;
        } finally {
            serviceLock.unlockWrite(stamp);
        }
    }

    /**
     * 获取服务的所有实例 - 线程安全读取
     */
    public List<ServiceInstance> getInstances(String serviceName) {
        if (serviceName == null) {
            return Collections.emptyList();
        }

        long stamp = serviceLock.tryOptimisticRead();
        List<ServiceInstance> instances = new ArrayList<>(serviceRegistry.getOrDefault(serviceName,
                Collections.emptyList()));

        if (!serviceLock.validate(stamp)) {
            stamp = serviceLock.readLock();
            try {
                instances = new ArrayList<>(serviceRegistry.getOrDefault(serviceName,
                        Collections.emptyList()));
            } finally {
                serviceLock.unlockRead(stamp);
            }
        }

        return instances;
    }

    /**
     * 获取所有服务名称 - 线程安全
     */
    public Set<String> getAllServiceNames() {
        long stamp = serviceLock.readLock();
        try {
            return new HashSet<>(serviceRegistry.keySet());
        } finally {
            serviceLock.unlockRead(stamp);
        }
    }

    /**
     * 获取所有服务实例 - 线程安全
     */
    public Map<String, List<ServiceInstance>> getAllServices() {
        long stamp = serviceLock.readLock();
        try {
            Map<String, List<ServiceInstance>> result = new HashMap<>();
            for (Map.Entry<String, List<ServiceInstance>> entry : serviceRegistry.entrySet()) {
                result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return result;
        } finally {
            serviceLock.unlockRead(stamp);
        }
    }

    // ==================== 配置操作 ====================

    /**
     * 发布配置 - 线程安全
     */
    public ConfigItem publishConfig(ConfigItem config) {
        if (config == null || config.getDataId() == null || config.getContent() == null) {
            throw new IllegalArgumentException("配置参数无效");
        }

        String dataId = config.getDataId();
        String group = config.getGroup() != null ? config.getGroup() : "DEFAULT_GROUP";
        String key = generateConfigKey(dataId, group);
        configLock.writeLock().lock();
        try {
            // 设置配置属性
            long version = configStore.getOrDefault(key, config).getVersion();
            if (version == 0) {
                config.setVersion(1);
            } else {
                config.setVersion(version + 1);
            }

            config.setMd5(calculateMD5(config.getContent()));
            config.setUpdateTime(System.currentTimeMillis());

            // 存储配置
            configStore.put(key, config);
            System.out.println("📝 配置发布成功: " + key + " v" + config.getVersion());
            // 通知监听器
            notifyConfigListeners(key, config);

            updateStatistics("configPublish");
            updateConfigCount();

            return config;
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * 获取配置 - 线程安全读取
     */
    public ConfigItem getConfig(String dataId, String group) {
        if (dataId == null) {
            return null;
        }

        String key = generateConfigKey(dataId, group != null ? group : "DEFAULT_GROUP");

        configLock.readLock().lock();
        try {
            ConfigItem config = configStore.get(key);
            if (config != null) {
                updateStatistics("configGet");
            }
            return config;
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * 删除配置 - 线程安全
     */
    public boolean deleteConfig(String dataId, String group) {
        if (dataId == null) {
            return false;
        }

        String key = generateConfigKey(dataId, group != null ? group : "DEFAULT_GROUP");

        configLock.writeLock().lock();
        try {
            ConfigItem removed = configStore.remove(key);
            if (removed != null) {
                updateStatistics("configDelete");
                updateConfigCount();
                return true;
            }
            return false;
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * 获取所有配置 - 线程安全
     */
    public List<ConfigItem> getAllConfigs() {
        configLock.readLock().lock();
        try {
            if (configStore.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<>(configStore.values());
        } finally {
            configLock.readLock().unlock();
        }
    }

    // ==================== 心跳管理 ====================

    /**
     * 更新心跳时间 - 线程安全
     */
    public void updateHeartbeat(ServiceInstance instance) {
        if (instance != null) {
            String instanceId = generateInstanceId(instance);
            heartbeatTimestamps.put(instanceId, System.currentTimeMillis());
            //todo if required here??
            registerInstance(instance);
            updateStatistics("heartbeat");
        }
    }

    /**
     * 获取心跳时间 - 线程安全
     */
    public Long getHeartbeatTime(String instanceId) {
        return instanceId != null ? heartbeatTimestamps.get(instanceId) : null;
    }

    /**
     * 获取所有心跳记录 - 线程安全
     */
    public Map<String, Long> getAllHeartbeatTimes() {
        return new HashMap<>(heartbeatTimestamps);
    }

    /**
     * 清理过期心跳 - 线程安全
     */
    public int cleanupExpiredHeartbeats(long threshold, long timeoutMillis) {
        int removedCount = 0;
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry : heartbeatTimestamps.entrySet()) {
            if (currentTime - entry.getValue() > timeoutMillis) {
                heartbeatTimestamps.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            updateStatistics("heartbeatCleanup", removedCount);
        }

        return removedCount;
    }

    // ==================== 监听器管理 ====================
    //todo  配置监听器一直未实现且未调用
    /**
     * 添加配置监听器 - 线程安全
     */
    public void addConfigListener(String dataId, String group, ConfigListener listener) {
        if (listener == null) return;

        String key = generateConfigKey(dataId, group != null ? group : "DEFAULT_GROUP");
        configListeners.computeIfAbsent(key, k -> new ArrayList<>()).add(listener);
        System.out.println("➕ 添加配置监听器: " + key + ", 当前监听器数: " +
                configListeners.get(key).size());
    }

    /**
     * 移除配置监听器 - 线程安全
     */
    public void removeConfigListener(String dataId, String group, ConfigListener listener) {
        if (listener == null) return;

        String key = generateConfigKey(dataId, group != null ? group : "DEFAULT_GROUP");
        List<ConfigListener> listeners = configListeners.get(key);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * 获取指定配置的监听器数量
     */
    public int getListenerCount(String dataId, String group) {
        String key = generateConfigKey(dataId, group != null ? group : "DEFAULT_GROUP");
        List<ConfigListener> listeners = configListeners.get(key);
        return listeners != null ? listeners.size() : 0;
    }

    /**
     * 获取所有配置的监听器统计
     */
    public Map<String, Integer> getAllListenerCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, List<ConfigListener>> entry : configListeners.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }

    // ==================== 工具方法 ====================

    /**
     * 清理所有数据 - 线程安全
     */
    public void clearAll() {
        long stamp = serviceLock.writeLock();
        try {
            serviceRegistry.clear();
            configStore.clear();
            heartbeatTimestamps.clear();
            configListeners.clear();
            instanceIdGenerator.set(1);
            statistics.clear();

            System.out.println("✅ 所有存储数据已清理");
        } finally {
            serviceLock.unlockWrite(stamp);
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        statistics.put("serviceCount", (long) serviceRegistry.size());
        statistics.put("configCount", (long) configStore.size());
        statistics.put("heartbeatCount", (long) heartbeatTimestamps.size());
        statistics.put("listenerCount", (long) configListeners.size());
        return new HashMap<>(statistics);
    }

    // ==================== 私有方法 ====================

    private String generateInstanceId(ServiceInstance instance) {
        return instance.getServiceName() + "-" +
                instance.getIp() + ":" + instance.getPort();
                //+ "-" + instanceIdGenerator.getAndIncrement();
    }

    private String generateConfigKey(String dataId, String group) {
        return dataId + ":" + group;
    }

    private String calculateMD5(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
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

    /**
     * 通知配置监听器
     */
    private void notifyConfigListeners(String key, ConfigItem config) {
        List<ConfigListener> listeners = configListeners.get(key);
        if (listeners != null && !listeners.isEmpty()) {
            for (ConfigListener listener : listeners) {
                try {
                    listener.onConfigChanged(key, config);
                } catch (Exception e) {
                    System.err.println("配置监听器通知失败: " + e.getMessage());
                }
            }
        }
    }

    private void updateStatistics(String operation) {
        updateStatistics(operation, 1);
    }

    private void updateStatistics(String operation, long increment) {
        statistics.compute("totalOperations", (k, v) ->
                (v == null ? 0L : (Long) v) + increment);

        statistics.compute(operation + "Count", (k, v) ->
                (v == null ? 0L : (Long) v) + increment);
    }

    private void updateServiceCount(String serviceName, int count) {
        statistics.put("lastUpdateTime", System.currentTimeMillis());
    }

    private void updateConfigCount() {
        statistics.put("configCount", (long) configStore.size());
    }

}
