package com.juhao666.demo.user.controller;

import com.juhao666.asac.model.Response;
import com.juhao666.asac.model.Result;
import com.juhao666.demo.user.model.User;
import com.juhao666.demo.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping("/users")
    public Result getAllUsers() {
        List<User> users = userService.getAllUsers();
        return Response.success("获取用户列表成功", users);
    }

    @GetMapping("/users/{id}")
    public Result getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Response.error("用户不存在");
        }
        return Response.success("获取用户成功", user);
    }

    @PostMapping("/users")
    public Result createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return Response.success("创建用户成功", createdUser);
    }
}
