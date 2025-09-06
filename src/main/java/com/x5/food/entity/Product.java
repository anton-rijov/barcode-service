package com.x5.food.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product", schema = "public")
public class Product {
    @Id
    @Column(name = "sku", nullable = false, length = 255)
    private String sku;

    @Column(name = "name", nullable = false, length = 1024)
    private String name;

    @OneToMany(mappedBy = "sku")
    private Set<Barcode> barcodes = new LinkedHashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(getSku(), product.getSku());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSku());
    }
}