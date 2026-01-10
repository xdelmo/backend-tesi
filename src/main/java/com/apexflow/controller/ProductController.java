package com.apexflow.controller;

import com.apexflow.model.Product;
import com.apexflow.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable String id) {
        return productRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    @PatchMapping("/{id}")
    public Product updateProduct(@PathVariable String id, @RequestBody Product productDetails) {
        return productRepository.findById(id).map(product -> {
            if (productDetails.getName() != null) product.setName(productDetails.getName());
            if (productDetails.getDescription() != null) product.setDescription(productDetails.getDescription());
            if (productDetails.getCategory() != null) product.setCategory(productDetails.getCategory());
            if (productDetails.getPrice() != null) product.setPrice(productDetails.getPrice());
            if (productDetails.getDuration() != null) product.setDuration(productDetails.getDuration());
            if (productDetails.getStatus() != null) product.setStatus(productDetails.getStatus());
            return productRepository.save(product);
        }).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable String id) {
        productRepository.deleteById(id);
    }
}
