package com.juhao666.demo.product.controller;

import com.juhao666.asac.model.Response;
import com.juhao666.asac.model.Result;
import com.juhao666.demo.product.service.ProductService;
import com.juhao666.demo.product.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    ProductService productService;

    @GetMapping("/products")
    public Result getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return  Response.success("获取商品列表成功", products);
    }


    @GetMapping("/products/{id}")
    public Result getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Response.error("商品不存在");
        }
        return Response.success("获取商品成功", product);
    }

    @PostMapping("/products")
    public Result createProduct(@RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return Response.success("创建商品成功", createdProduct);
    }

}
