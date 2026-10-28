package com.cardock.dto;

import com.cardock.entity.CarStatus;
import com.cardock.entity.FuelType;
import com.cardock.entity.Transmission;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CarRequest(
        @NotBlank @Size(max = 60) String brand,
        @NotBlank @Size(max = 100) String model,
        @NotNull @Min(1900) Integer manufacturingYear,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull FuelType fuelType,
        @NotNull Transmission transmission,
        @NotNull @Min(0) Integer mileageKm,
        @Size(max = 1000) String description,
        @NotNull CarStatus status) {
}
