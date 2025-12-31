package com.juhao666.demo.user;

import com.juhao666.asac.annotation.EnableAsAc;
import com.juhao666.asac.service.RegistrationService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAsAc
public class UserServiceApplication {
    private static final int PORT = 8001;

    @Autowired
    private RegistrationService registrationService;

    @PostConstruct
    public void init() {
        registrationService.registerToRegistry();
    }

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("==========================================");
        System.out.println("用户服务启动成功！");
        System.out.println("端口: " + PORT);
        System.out.println("API文档:");
        System.out.println("  - 获取所有用户: GET /api/users");
        System.out.println("  - 根据ID获取用户: GET /api/users/{id}");
        System.out.println("  - 创建用户: POST /api/users");
        System.out.println("  - 发现商品服务: GET /api/discover/product-service");
        System.out.println("  - 发现订单服务: GET /api/discover/order-service");
        System.out.println("  - 健康检查: GET /api/health");
        System.out.println("==========================================");
    }


}
