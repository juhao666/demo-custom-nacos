package com.juhao666.demo;

import com.juhao666.demo.model.ServiceInstance;
import com.juhao666.demo.store.RegistryStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling

public class Application {
// 服务注册表：serviceName -> List<ServiceInstance>
//    private static final Map<String, List<ServiceInstance>> SERVICE_REGISTRY = new ConcurrentHashMap<>();
//
//    // 心跳时间戳：instanceId -> lastHeartbeatTime
//    private static final Map<String, Long> HEARTBEAT_TIMESTAMPS = new ConcurrentHashMap<>();
//
//    // 健康检查阈值（30秒）
//    private static final long HEALTH_CHECK_THRESHOLD = 30000;
//
//    // 心跳线程池
//    private static final ScheduledExecutorService HEARTBEAT_EXECUTOR = Executors.newScheduledThreadPool(2);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
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
        //startHealthCheckTask();
    }

//    private static void startHealthCheckTask() {
//        HEARTBEAT_EXECUTOR.scheduleAtFixedRate(() -> {
//            long currentTime = System.currentTimeMillis();
//            int removedCount = 0;
//
//            for (Map.Entry<String, List<ServiceInstance>> entry : SERVICE_REGISTRY.entrySet()) {
//                String serviceName = entry.getKey();
//                List<ServiceInstance> instances = entry.getValue();
//
//                Iterator<ServiceInstance> iterator = instances.iterator();
//                while (iterator.hasNext()) {
//                    ServiceInstance instance = iterator.next();
//                    String instanceId = instance.getInstanceId();
//                    Long lastHeartbeat = HEARTBEAT_TIMESTAMPS.get(instanceId);
//
//                    if (lastHeartbeat == null ||
//                            (currentTime - lastHeartbeat) > HEALTH_CHECK_THRESHOLD) {
//                        // 实例不健康，移除
//                        iterator.remove();
//                        HEARTBEAT_TIMESTAMPS.remove(instanceId);
//                        removedCount++;
//                        System.out.println("⚠️ 移除不健康实例: " + serviceName + " [" + instanceId + "]");
//                    }
//                }
//
//                // 如果服务没有实例了，移除服务
//                if (instances.isEmpty()) {
//                    SERVICE_REGISTRY.remove(serviceName);
//                }
//            }
//
//            if (removedCount > 0) {
//                System.out.println("健康检查完成，移除 " + removedCount + " 个不健康实例");
//            }
//        }, 10, 10, TimeUnit.SECONDS);
//    }


    /////////////////////////////////
    @Autowired
    private RegistryStore registryStore;

    // 心跳线程池
    private static final ScheduledExecutorService HEALTH_CHECK_EXECUTOR =
            Executors.newScheduledThreadPool(2);


    /**
     * 初始化阶段回调
     * 执行阶段：Bean 实例化完成 →
     * 依赖注入完成​ →
     * 执行 @PostConstruct方法(e.g.载配置、建立数据库连接、初始化缓存等) →
     * 其他初始化逻辑（如 InitializingBean.afterPropertiesSet()）。**
     */
    @PostConstruct
    public void init() {
        // 启动健康检查任务
        startHealthCheckTask();
        System.out.println("✅ 注册中心初始化完成，健康检查任务已启动");
    }

    /**
     * 销毁时清理资源
     * 执行阶段：Spring 容器关闭 →
     *          触发 @PreDestroy方法 →
     *          其他销毁逻辑（如 DisposableBean.destroy()）。
     */
    @PreDestroy
    public void destroy() {
        HEALTH_CHECK_EXECUTOR.shutdown();
        try {
            if (!HEALTH_CHECK_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                HEALTH_CHECK_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            HEALTH_CHECK_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("🛑 健康检查任务已停止");
    }
    /**
     * 修改后的健康检查任务，从RegistryStore中获取数据
     */
    private void startHealthCheckTask() {
        HEALTH_CHECK_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                int removedCount = 0;

                // 从RegistryStore获取所有服务
                Map<String, List<ServiceInstance>> allServices =
                        registryStore.getAllServices();

                for (Map.Entry<String, List<ServiceInstance>> entry : allServices.entrySet()) {
                    String serviceName = entry.getKey();
                    List<ServiceInstance> instances = entry.getValue();

                    // 遍历实例检查心跳
                    for (ServiceInstance instance : instances) {
                        String instanceId = instance.getInstanceId();
                        Long lastHeartbeat = registryStore.getHeartbeatTime(instanceId);

                        // 30秒内没有心跳视为不健康
                        if (lastHeartbeat == null ||
                                (currentTime - lastHeartbeat) > 30000) {

                            // 从RegistryStore中注销实例
                            boolean deregistered = registryStore.deregisterInstance(serviceName, instanceId);
                            if (deregistered) {
                                removedCount++;
                                System.out.println("⚠️ 移除不健康实例: " + serviceName + " [" + instanceId + "]");
                            }
                        }
                    }
                }

                if (removedCount > 0) {
                    System.out.println("健康检查完成，移除 " + removedCount + " 个不健康实例");
                }

            } catch (Exception e) {
                System.err.println("健康检查任务异常: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

}
