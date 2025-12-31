package com.juhao666.demo.user.controller;

import com.juhao666.asac.model.Response;
import com.juhao666.asac.model.Result;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DiscoverController {
    //先注释掉，服务发现应该是server端负责的
//    @GetMapping("/discover/product-service")
//    public Result discoverProductService() {
//        List<ServiceInstance> instances = serviceCache.get("product-service");
//        if (instances == null || instances.isEmpty()) {
//            return Response.error("未发现商品服务实例");
//        }
//        return Response.success("发现商品服务成功", instances);
//    }
//
//    @GetMapping("/discover/order-service")
//    public Result discoverOrderService() {
//        List<ServiceInstance> instances = serviceCache.get("order-service");
//        if (instances == null || instances.isEmpty()) {
//            return Response.error("未发现订单服务实例");
//        }
//        return Response.success("发现订单服务成功", instances);
//    }
}
