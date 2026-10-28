package com.cardock.dto;

import com.cardock.entity.Car;
import com.cardock.entity.CarStatus;
import com.cardock.entity.FuelType;
import com.cardock.entity.Transmission;

import java.math.BigDecimal;

public record CarResponse(
        Long id,
        String brand,
        String model,
        Integer manufacturingYear,
        BigDecimal price,
        FuelType fuelType,
        Transmission transmission,
        Integer mileageKm,
        String description,
        CarStatus status) {

    public static CarResponse from(Car car) {
        return new CarResponse(
                car.getId(), car.getBrand(), car.getModel(), car.getManufacturingYear(),
                car.getPrice(), car.getFuelType(), car.getTransmission(), car.getMileageKm(),
                car.getDescription(), car.getStatus());
    }
}
