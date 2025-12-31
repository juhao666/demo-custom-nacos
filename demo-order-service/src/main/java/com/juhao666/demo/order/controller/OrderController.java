package com.juhao666.demo.order.controller;

import com.juhao666.asac.model.Result;
import com.juhao666.asac.model.Response;
import com.juhao666.asac.model.ServiceInstance;
import com.juhao666.demo.order.model.Order;
import com.juhao666.demo.order.service.DiscoverService;
import com.juhao666.demo.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    OrderService orderService;

    @Autowired
    DiscoverService discoverService;

    @Autowired
    RestTemplate restTemplate;

    @GetMapping("/orders")
    public Result getAllorders() {
        List<Order> orders = orderService.getAllOrders();
        return Response.success("获取商品列表成功", orders);
    }

    @GetMapping("/orders/{id}")
    public Result getorderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return Response.error("商品不存在");
        }
        return Response.success("获取商品成功", order);
    }

    @PostMapping("/orders")
    public Result createorder(@RequestBody Order order) {
        Order createdOrder = orderService.createOrder(order);
        return Response.success("创建商品成功", order);
    }
    /**
     * 获取商品详情（演示服务间调用）
     */
    @GetMapping("/orders/{id}/detail")
    public Result getOrderDetail(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return Response.error("商品不存在");
        }

        // 尝试获取用户信息（模拟商品创建者）
        // 首先发现用户服务
        Result discoveryResult = discoverService.findActiveService();
        if (!discoveryResult.isSuccess()) {
            return Response.success("获取商品成功，但无法获取用户信息", order);
        }

        // 获取用户服务实例
        List<ServiceInstance> userInstances = (List<ServiceInstance>) discoveryResult.getData();
        if (userInstances == null || userInstances.isEmpty()) {
            return Response.success("获取商品成功，但用户服务不可用", order);
        }

        // 使用第一个用户服务实例 todo  使用指定的user-service
        ServiceInstance userInstance = userInstances.get(0);
        String userServiceUrl = "http://" + userInstance.getIp() + ":" + userInstance.getPort() + "/api";
        System.out.printf("调用用户服务 url= %s %n", userServiceUrl);
        try {
            // 调用用户服务获取用户信息
            Result userResult = restTemplate.getForObject(
                    userServiceUrl + "/users/1",
                    Result.class
            );

            Map<String, Object> detail = new HashMap<>();
            detail.put("order", order);
            detail.put("userService", userInstance);

            if (userResult != null && userResult.isSuccess()) {
                detail.put("creator", userResult.getData());
            } else {
                detail.put("creator", "获取用户信息失败");
            }

            return Response.success("获取商品详情成功", detail);
        } catch (Exception e) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("order", order);
            detail.put("error", "调用用户服务失败: " + e.getMessage());
            return Response.success("获取商品详情成功（用户服务调用失败）", detail);
        }
    }

}
