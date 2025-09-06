package com.x5.food.dto.projection;


import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public interface ProductProjection {
    String getSku();

    String getName();

    @Value("#{target.barcodes.![barcode]}")
    List<String> getBarcodes();
}