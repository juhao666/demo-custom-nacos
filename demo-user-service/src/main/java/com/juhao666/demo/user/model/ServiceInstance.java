package com.juhao666.demo.user.model;

public class ServiceInstance {
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
