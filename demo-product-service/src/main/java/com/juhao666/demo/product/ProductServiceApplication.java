package com.juhao666.demo.product;

import com.juhao666.asac.annotation.EnableAsAc;
import com.juhao666.asac.service.RegistrationService;
import com.juhao666.demo.product.model.Result;
import com.juhao666.demo.product.model.ServiceInstance;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
//@ComponentScan({"com.juhao666.asac", "com.juhao666.demo"})
@EnableAsAc
public class ProductServiceApplication {
	private static final int PORT = 8002;

	@Autowired
	private RegistrationService registrationService;

	@PostConstruct
	public void init() {
		registrationService.registerToRegistry();
	}


	// 心跳线程池
	private final ScheduledExecutorService heartbeatExecutor =
			Executors.newSingleThreadScheduledExecutor();

	public static void main(String[] args) {
		SpringApplication.run(ProductServiceApplication.class, args);
		System.out.println("==========================================");
		System.out.println("商品服务启动成功！");
		System.out.println("端口: " + PORT);
		System.out.println("API文档:");
		System.out.println("  - 获取所有商品: GET /api/products");
		System.out.println("  - 根据ID获取商品: GET /api/products/{id}");
		System.out.println("  - 创建商品: POST /api/products");
		System.out.println("  - 发现用户服务: GET /api/discover/user-service");
		System.out.println("==========================================");
	}


}

