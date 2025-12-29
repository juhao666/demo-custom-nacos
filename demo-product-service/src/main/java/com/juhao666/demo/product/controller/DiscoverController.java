package com.juhao666.demo.product.controller;

import com.juhao666.demo.product.model.Product;
import com.juhao666.demo.product.model.Result;
import com.juhao666.demo.product.service.DiscoverService;
import com.juhao666.demo.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
