package com.juhao666.asac.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "asac")
public class AsAcProperties {
    private boolean enabled = true;
    private String registryUrl = "http://localhost:8848/api/v1";
    private String serviceName;
    private String ip = "localhost";
    private int port;
    private long heartbeatInterval = 5000; // milliseconds
}