package com.x5.food.repository;

import com.x5.food.dto.projection.BarcodeStatisticProjection;
import com.x5.food.dto.projection.ProductProjection;
import com.x5.food.entity.Barcode;
import com.x5.food.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarcodeRepository extends JpaRepository<Barcode, String> {

    // Поиск всех штрихкодов для определенного продукта
    List<Barcode> findBySku(Product product);

    // Поиск всех штрихкодов по SKU
    List<Barcode> findBySku_Sku(String sku);

    // Поиск продукта по штрихкоду
    @Query("select p from Product p join p.barcodes b where b.barcode = :barcode")
    Optional<ProductProjection> findProductByBarcode(@Param("barcode") String barcode);

    // Проверка существования штрихкода
    boolean existsByBarcode(String barcode);

    // Количество штрихкодов для определенного продукта
    long countBySku(Product product);

    // Удаление всех штрихкодов для определенного продукта
    void deleteBySku(Product product);

    // Удаление по штрихкоду
    void deleteByBarcode(String barcode);

    // Статистика по всем штрихкодам
    @Query("select count(b) as barcodesCount, count(distinct b.sku) as skuCount from Barcode b")
    Optional<BarcodeStatisticProjection> getBarcodeAndSkuCounts();
}