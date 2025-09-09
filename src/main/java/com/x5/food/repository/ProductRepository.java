package com.x5.food.repository;

import com.x5.food.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query("SELECT p FROM Product p JOIN FETCH p.barcodes WHERE p.sku = :sku")
    Optional<Product> findBySkuWithBarcodes(@Param("sku") String sku);

    @Query("SELECT p FROM Product p JOIN p.barcodes b WHERE b.barcode = :barcode")
    Optional<Product> findByBarcode(@Param("barcode") String barcode);

    @Modifying
    @Query(value = """
            insert into public.product (sku, name) 
            values (:sku, :name)
            on conflict (sku) do update set name = excluded.name
            """, nativeQuery = true)
    void upsertProduct(@Param("sku") String sku, @Param("name") String name);
}