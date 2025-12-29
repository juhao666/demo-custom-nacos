package com.juhao666.demo.product.service;

import com.juhao666.demo.product.model.Product;
import com.juhao666.demo.product.model.ServiceInstance;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProductService {
    // 服务发现缓存
    private final Map<String, List<ServiceInstance>> serviceCache = new ConcurrentHashMap<>();
    // 内存存储商品数据
    private final Map<Long, Product> productDatabase = new ConcurrentHashMap<>();
    @PostConstruct
    public void initData() {
        // 初始化测试数据
        productDatabase.put(1L, new Product(1L, "iPhone 15 Pro", 8999.00, "苹果最新旗舰手机", "电子产品", 100));
        productDatabase.put(2L, new Product(2L, "MacBook Pro 16", 18999.00, "高性能笔记本电脑", "电子产品", 50));
        productDatabase.put(3L, new Product(3L, "AirPods Pro 2", 1499.00, "主动降噪无线耳机", "电子产品", 200));
        productDatabase.put(4L, new Product(4L, "小米电视 75寸", 4999.00, "4K超高清智能电视", "家电", 30));
        productDatabase.put(5L, new Product(5L, "海尔冰箱", 3999.00, "对开门节能冰箱", "家电", 40));

        System.out.println("✅ 初始化测试数据完成，共 " + productDatabase.size() + " 个商品");
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(productDatabase.values());
    }

    public Product getProductById(Long id) {
        return productDatabase.get(id);
    }

    public Product createProduct(Product product) {
        Long newId = productDatabase.keySet().stream()
                .max(Long::compareTo)
                .orElse(0L) + 1;
        product.setId(newId);
        productDatabase.put(newId, product);
        return getProductById(newId);
    }
}
