package com.juhao666.demo.order;

import com.juhao666.asac.annotation.EnableAsAc;
import com.juhao666.asac.client.RegistrationService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAsAc
public class OrderServiceApplication {
    private static final int PORT = 8003;
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("==========================================");
        System.out.println("商品服务启动成功！");
        System.out.println("端口: " + PORT);
        System.out.println("API文档:");
        System.out.println("  - 获取所有商品: GET /api/orders");
        System.out.println("  - 根据ID获取商品: GET /api/orders/{id}");
        System.out.println("  - 创建商品: POST /api/orders");
        System.out.println("  - 发现用户服务: GET /api/discover/user-service");
        System.out.println("  - 获取商品详情（带用户信息）: GET /api/orders/{id}/detail");
        System.out.println("==========================================");
    }

}

