package com.juhao666.demo.order.controller;

import com.juhao666.demo.order.model.Result;
import com.juhao666.demo.order.service.DiscoverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DiscoverController {

    @Autowired
    DiscoverService discoverService;
    @GetMapping("/discover/user-service")
    public Result discoverUserService() {
      return discoverService.findActiveService();
    }
}
