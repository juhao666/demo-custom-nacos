package com.juhao666.demo.user.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ConfigListener implements CommandLineRunner {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    private final AtomicReference<String> currentMd5 = new AtomicReference<>();
    private final ConcurrentHashMap<String, String> configProperties = new ConcurrentHashMap<>();
    private volatile boolean listening = false;
    private Thread listenerThread;


    // 注册中心地址
    private final String registryUrl = "http://localhost:8848";

    // 监听的配置
    //todo dataID should be configured
    //by default,  [spring.application.name].yml or properties
    private final String dataId = "application.properties";
    private final String group = "DEFAULT_GROUP";


    @PostConstruct
    public void init() {
        System.out.println("🚀 配置监听器初始化...");
        // 初始化时获取配置
        fetchInitialConfig();
        // 启动监听线程
        startListening();
    }

    @PreDestroy
    public void destroy() {
        stopListening();
        System.out.println("🛑 配置监听器已停止");
    }

    /**
     * 停止配置监听
     */
    public void stopListening() {
        listening = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    /**
     * 获取初始配置
     */
    private void fetchInitialConfig() {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(registryUrl)
                    .path("/api/v1/config")
                    .queryParam("dataId", dataId)
                    .queryParam("group", group)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            if (Boolean.TRUE.equals(result.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configData = (Map<String, Object>) result.get("data");
                if (configData != null) {
                    String content = (String) configData.get("content");
                    currentMd5.set((String) configData.get("md5"));

                    // 解析配置
                    parseAndUpdateConfig(content);

                    System.out.println("✅ 获取到初始配置: " + configData.get("dataId"));
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 获取初始配置失败: " + e.getMessage());
        }
    }

    /**
     * 启动配置监听
     */
    public void startListening() {
        if (listening) {
            return;
        }

        listening = true;
        listenerThread = new Thread(() -> {
            System.out.println("🔍 开始监听配置变更...");

            while (listening) {
                try {
                    longPolling();//todo
                } catch (Exception e) {
                    if (listening) {
                        System.err.println("配置监听异常: " + e.getMessage());
                        try {
                            Thread.sleep(5000); // 等待5秒后重试
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }, "Config-Listener-Thread");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * 长轮询配置变更
     */
    private void longPolling() {
        String url = UriComponentsBuilder.fromHttpUrl(registryUrl)
                .path("/api/v1/config/listener")
                .queryParam("dataId", dataId)
                .queryParam("group", group)
                .queryParam("md5", currentMd5.get())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("User-Agent", "SpringBootConfigClient/1.0");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            String response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            ).getBody();

            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            if (Boolean.TRUE.equals(result.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> configData = (Map<String, Object>) result.get("data");
                if (configData != null) {
                    handleConfigChange(configData);
                }
            } else if ("监听超时".equals(result.get("message"))) {
                // 长轮询超时，继续下一次
                System.out.println("⏰ 长轮询超时，继续监听...");
            }
        } catch (ResourceAccessException e) {
            // 连接超时，继续下一次
            System.out.println("⏰ 长轮询超时，继续监听...");
            //todo
            //Thread.sleep(5000);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理配置变更
     */
    private void handleConfigChange(Map<String, Object> configData) {
        String newMd5 = (String) configData.get("md5");
        String content = (String) configData.get("content");
        long version = ((Number) configData.get("version")).longValue();

        System.out.println("🔄 检测到配置变更:");
        System.out.println("  - DataId: " + configData.get("dataId"));
        System.out.println("  - Version: " + version);
        System.out.println("  - MD5: " + newMd5);

        // 更新MD5
        currentMd5.set(newMd5);

        // 解析并更新配置
        parseAndUpdateConfig(content);

        // 触发配置刷新事件
        onConfigRefreshed();
    }

    /**
     * 解析并更新配置
     */
    private void parseAndUpdateConfig(String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String[] parts = trimmed.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                configProperties.put(key, value);
            }
        }

        System.out.println("✅ 配置已更新，当前配置项数量: " + configProperties.size());
    }

    /**
     * 配置刷新回调
     */
    private void onConfigRefreshed() {
        // 这里可以触发Spring的EnvironmentChangeEvent
        // 或者刷新@ConfigurationProperties bean

        System.out.println("🔄 触发配置刷新...");

        // 示例：打印所有配置
        configProperties.forEach((key, value) -> {
            System.out.println("  " + key + " = " + value);
        });
        refresh();//*** refresh bean with changed properties
    }

    @Autowired
    private ConfigurableEnvironment environment;
    private void refreshConfigProperties() {
        // 移除旧的 property source（如果存在）
        environment.getPropertySources().remove("dynamicConfig");

        // 创建新的 MapPropertySource
        MapPropertySource propertySource = new MapPropertySource(
                "dynamicConfig",
                new HashMap<>(configProperties) // 深拷贝避免并发问题
        );

        // 添加到 environment 最前面（优先级高）
        environment.getPropertySources().addFirst(propertySource);
    }

    @Autowired
    private ContextRefresher contextRefresher;

    private void refresh() {
        refreshConfigProperties(); // 先更新 Environment
        Set<String> keys = contextRefresher.refresh(); // 触发 @RefreshScope Bean 重建
        System.out.println("Refreshed keys: " + keys);
    }
    /**
     * 获取配置值
     */
    public String getProperty(String key) {
        return configProperties.get(key);
    }

    public String getProperty(String key, String defaultValue) {
        return configProperties.getOrDefault(key, defaultValue);
    }

    /**
     * 获取所有配置
     */
    public Map<String, String> getAllProperties() {
        return new java.util.HashMap<>(configProperties);
    }


    @Override
    public void run(String... args) {
        System.out.println("==========================================");
        System.out.println("Spring Boot 配置监听客户端启动完成");
        System.out.println("监听配置: " + dataId);
        System.out.println("注册中心: " + registryUrl);
        System.out.println("当前配置:");
        configProperties.forEach((key, value) -> {
            System.out.println("  " + key + " = " + value);
        });
        System.out.println("==========================================");
    }

}
