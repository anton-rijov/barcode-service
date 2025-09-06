package com.x5.food.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "barcode", schema = "public")
public class Barcode {
    @Id
    @Column(name = "barcode", nullable = false, length = 255)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku")
    private Product sku;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Barcode barcode1 = (Barcode) o;
        return Objects.equals(getBarcode(), barcode1.getBarcode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBarcode());
    }

    @Override
    public String toString() {
        return "Barcode{" +
                "barcode='" + barcode + '\'' +
                ", sku=" + sku +
                '}';
    }
}