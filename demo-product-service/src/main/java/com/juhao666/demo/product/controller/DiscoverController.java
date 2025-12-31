package com.juhao666.demo.product.controller;

import com.juhao666.asac.model.Result;
import com.juhao666.demo.product.service.DiscoverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
