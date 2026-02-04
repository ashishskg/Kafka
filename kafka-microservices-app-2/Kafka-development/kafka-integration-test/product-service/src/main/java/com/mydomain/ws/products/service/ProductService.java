package com.mydomain.ws.products.service;


import com.mydomain.ws.products.model.CreateProductRestModel;

public interface ProductService {
    String createProduct(CreateProductRestModel productRestModel) throws Exception;
}
