package com.x5.food.dto;

import com.x5.food.entity.Barcode;
import com.x5.food.entity.Product;

import java.util.List;
import java.util.stream.Collectors;

public record ProductResponse(
        String sku,
        String name,
        List<String> barcodes
) {

    public static ProductResponse fromEntity(Product product) {
        List<String> barcodeList = product.getBarcodes().stream()
                .map(Barcode::getBarcode)
                .collect(Collectors.toList());

        return new ProductResponse(
                product.getSku(),
                product.getName(),
                barcodeList
        );
    }

    public static ProductResponse fromExternal(OpenFoodFactsResponse externalResponse, String barcode) {
        var product = externalResponse.product();

        String name = buildProductName(product);
        String sku = "SKU_" + getLastSixDigits(barcode);

        return new ProductResponse(
                sku,
                name,
                List.of(barcode)
        );
    }

    private static String buildProductName(OpenFoodFactsResponse.Product product) {
        StringBuilder nameBuilder = new StringBuilder();

        if (product.productName() != null && !product.productName().isEmpty()) {
            nameBuilder.append(product.productName());
        } else if (product.brands() != null && !product.brands().isEmpty()) {
            nameBuilder.append(product.brands());
        } else {
            nameBuilder.append("Unknown Product");
        }

        if (product.quantity() != null && !product.quantity().isEmpty()) {
            nameBuilder.append(" ").append(product.quantity());
        }

        return nameBuilder.toString();
    }

    private static String getLastSixDigits(String barcode) {
        if (barcode == null || barcode.length() < 6) {
            return barcode != null ? barcode : "000000";
        }
        return barcode.substring(barcode.length() - 6);
    }
}
