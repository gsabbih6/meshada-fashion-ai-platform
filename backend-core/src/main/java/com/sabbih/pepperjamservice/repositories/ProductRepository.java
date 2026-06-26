package com.sabbih.pepperjamservice.repositories;

import com.sabbih.pepperjamservice.DModels.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySKU(String sku);
    Optional<Product> findBySKU(String sku);
    List<Product> findByVideoGeneratedFalse();
}
