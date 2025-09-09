package com.x5.food.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenFoodFactsResponse(
        @JsonProperty("code") String barcode,
        @JsonProperty("product") Product product
) {
    public record Product(
            @JsonProperty("product_name") String productName,
            @JsonProperty("quantity") String quantity,
            @JsonProperty("brands") String brands,
            @JsonProperty("nutriments") Nutriments nutriments
    ) {
    }

    public record Nutriments(
            @JsonProperty("energy-kcal_100g") Double energyKcal
    ) {
    }
}