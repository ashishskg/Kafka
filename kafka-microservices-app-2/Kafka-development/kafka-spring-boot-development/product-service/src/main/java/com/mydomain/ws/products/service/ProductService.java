package com.mydomain.ws.products.service;


import com.mydomain.ws.common.Product;

public interface ProductService {
    String createProduct(Product product);
    String createProductSynchronously(Product product) throws Exception;
}
