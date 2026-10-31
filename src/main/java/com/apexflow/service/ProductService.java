package com.apexflow.service;

import com.apexflow.model.Product;
import com.apexflow.repository.ProductRepository;
import com.apexflow.util.QueryFilterParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> getProducts(Map<String, String> params) {
        Pageable pageable = QueryFilterParser.getPageable(params);
        Specification<Product> spec = QueryFilterParser.parse(params);
        return productRepository.findAll(spec, pageable);
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product createProduct(Product product) {
        if (product.getId() == null || product.getId().isBlank()) {
            product.setId("PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        return productRepository.save(product);
    }

    @Transactional
    public Optional<Product> updateProduct(String id, Product details) {
        return productRepository.findById(id).map(p -> {
            if (details.getName() != null) p.setName(details.getName());
            if (details.getDescription() != null) p.setDescription(details.getDescription());
            if (details.getCategory() != null) p.setCategory(details.getCategory());
            if (details.getPrice() != null) p.setPrice(details.getPrice());
            if (details.getDuration() != null) p.setDuration(details.getDuration());
            if (details.getStatus() != null) p.setStatus(details.getStatus());
            return productRepository.save(p);
        });
    }

    @Transactional
    public boolean deleteProduct(String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
