package com.juhao666.demo.user.service;

import com.juhao666.asac.model.ServiceInstance;
import com.juhao666.demo.user.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    // 服务发现缓存
    private final Map<String, List<ServiceInstance>> serviceCache = new ConcurrentHashMap<>();

    // 内存存储用户数据
    private final Map<Long, User> userDatabase = new ConcurrentHashMap<>();

    @PostConstruct
    public void initData() {
        // 初始化测试数据
        userDatabase.put(1L, new User(1L, "张三", "zhangsan@example.com", "13800138001"));
        userDatabase.put(2L, new User(2L, "李四", "lisi@example.com", "13800138002"));
        userDatabase.put(3L, new User(3L, "王五", "wangwu@example.com", "13800138003"));
        System.out.println("✅ 初始化测试数据完成，共 " + userDatabase.size() + " 个用户");
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(userDatabase.values());
    }

    public User getUserById(Long id) {
        return userDatabase.get(id);
    }

    public User createUser(User user) {
        //userId递增
        Long newId = userDatabase.keySet().stream()
                .max(Long::compareTo)
                .orElse(0L) + 1;
        user.setId(newId);
        userDatabase.put(newId, user);
        return getUserById(newId);
    }
}
