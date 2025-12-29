package com.juhao666.demo.model;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.HashMap;
import java.util.Map;


public class ServiceInstance {
    @NotBlank(message="serviceName不能为空")
    private String serviceName;      // 服务名
    private String instanceId;       // 实例ID
    @NotBlank(message="ip不能为空")
    private String ip;               // IP地址
    @Min(value=0,message = "port必须大于0")
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
