package com.x5.food.repository;

import com.x5.food.dto.projection.BarcodeStatisticProjection;
import com.x5.food.entity.Barcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BarcodeRepository extends JpaRepository<Barcode, String> {

    @Query("SELECT COUNT(b) as barcodesCount, COUNT(DISTINCT b.sku) as skuCount FROM Barcode b")
    Optional<BarcodeStatisticProjection> getBarcodeStatistics();
}