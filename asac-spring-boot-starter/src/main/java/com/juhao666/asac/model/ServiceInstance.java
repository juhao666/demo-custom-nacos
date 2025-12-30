package com.juhao666.asac.model;


import lombok.Data;

@Data
public class ServiceInstance {
    private String serviceName;
    private String instanceId;
    private String ip;
    private int port;
    private String status;
}
