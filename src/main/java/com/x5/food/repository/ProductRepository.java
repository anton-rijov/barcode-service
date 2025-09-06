package com.x5.food.repository;

import com.x5.food.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // Поиск по имени (точное совпадение)
    Optional<Product> findByName(String name);

    // Поиск по части имени (без учета регистра)
    List<Product> findByNameContainingIgnoreCase(String name);

    // Поиск продуктов, содержащих в имени указанную строку
    @Query("select p from Product p where p.name ilike :namePart")
    List<Product> findByNamePart(@Param("namePart") String namePart);

    // Проверка существования продукта по SKU
    boolean existsBySku(String sku);

    // Получение количества продуктов
    long count();

    // Поиск всех продуктов с сортировкой по имени
    List<Product> findAllByOrderByNameAsc();
}