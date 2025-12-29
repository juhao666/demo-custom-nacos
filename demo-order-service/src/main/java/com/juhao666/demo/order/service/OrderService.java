package com.juhao666.demo.order.service;

import com.juhao666.demo.order.model.Order;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {
    // 内存存储商品数据
    private final Map<Long, Order> orderDatabase = new ConcurrentHashMap<>();

    @PostConstruct
    public void initData() {
        // 初始化测试数据
        orderDatabase.put(1L, new Order(1L, "iPhone 15 Pro", 8999.00, "苹果最新旗舰手机", "电子产品", 100));
        orderDatabase.put(2L, new Order(2L, "MacBook Pro 16", 18999.00, "高性能笔记本电脑", "电子产品", 50));
        orderDatabase.put(3L, new Order(3L, "AirPods Pro 2", 1499.00, "主动降噪无线耳机", "电子产品", 200));
        orderDatabase.put(4L, new Order(4L, "小米电视 75寸", 4999.00, "4K超高清智能电视", "家电", 30));
        orderDatabase.put(5L, new Order(5L, "海尔冰箱", 3999.00, "对开门节能冰箱", "家电", 40));

        System.out.println("✅ 初始化测试数据完成，共 " + orderDatabase.size() + " 个商品");
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orderDatabase.values());
    }

    public Order getOrderById(Long id) {
        return orderDatabase.get(id);
    }

    public Order createOrder(Order order) {
        Long newId = orderDatabase.keySet().stream()
                .max(Long::compareTo)
                .orElse(0L) + 1;
        order.setId(newId);
        orderDatabase.put(newId, order);
        return getOrderById(newId);
    }
}
