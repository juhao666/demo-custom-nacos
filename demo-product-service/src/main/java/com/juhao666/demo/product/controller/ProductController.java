package com.juhao666.demo.product.controller;

import com.juhao666.demo.product.service.ProductService;
import com.juhao666.demo.product.model.Product;
import com.juhao666.demo.product.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    ProductService productService;

    @GetMapping("/products")
    public Result getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return Result.success("获取商品列表成功", products);
    }


    @GetMapping("/products/{id}")
    public Result getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success("获取商品成功", product);
    }

    @PostMapping("/products")
    public Result createProduct(@RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return Result.success("创建商品成功", product);
    }

}
