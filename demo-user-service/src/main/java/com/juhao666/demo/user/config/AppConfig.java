package com.juhao666.demo.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 示例配置类
 */
@Component
@ConfigurationProperties(prefix = "app")
class AppConfig {
    private String name;
    private String version;
    private String environment;

    // 配置变更时，Spring会自动刷新这个bean
    // 需要配合@RefreshScope注解

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @Override
    public String toString() {
        return "AppConfig{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", environment='" + environment + '\'' +
                '}';
    }
}
