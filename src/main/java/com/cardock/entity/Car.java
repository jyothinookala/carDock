package com.cardock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "manufacturing_year", nullable = false)
    private Integer manufacturingYear;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Transmission transmission;

    @Column(name = "mileage_km", nullable = false)
    private Integer mileageKm;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Car() {
    }

    public Car(String brand, String model, Integer manufacturingYear, BigDecimal price,
               FuelType fuelType, Transmission transmission, Integer mileageKm,
               String description, CarStatus status) {
        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.price = price;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.mileageKm = mileageKm;
        this.description = description;
        this.status = status;
    }

    @PrePersist
    void setCreatedAt() {
        createdAt = LocalDateTime.now();
    }

    public void updateDetails(String brand, String model, Integer manufacturingYear, BigDecimal price,
                              FuelType fuelType, Transmission transmission, Integer mileageKm,
                              String description, CarStatus status) {
        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.price = price;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.mileageKm = mileageKm;
        this.description = description;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public Integer getManufacturingYear() {
        return manufacturingYear;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public Transmission getTransmission() {
        return transmission;
    }

    public Integer getMileageKm() {
        return mileageKm;
    }

    public String getDescription() {
        return description;
    }

    public CarStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
