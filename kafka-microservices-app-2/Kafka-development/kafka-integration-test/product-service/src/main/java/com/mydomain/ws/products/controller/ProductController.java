package com.mydomain.ws.products.controller;

import com.mydomain.ws.products.exception.ErrorMessage;
import com.mydomain.ws.products.model.CreateProductRestModel;
import com.mydomain.ws.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Object> createProduct(@RequestBody CreateProductRestModel product) throws Exception {
//        log.info("Creating product {}", product);
        log.info("Creating product " +  product);
        String productId;

        try {
            productId = productService.createProduct(product);
        } catch (Exception e) {
            log.error("Error creating product: " + e.getMessage());
            ErrorMessage errorMessage = new ErrorMessage(new Date(), e.getMessage(), "/products");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }
}
