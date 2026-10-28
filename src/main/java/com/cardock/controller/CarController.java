package com.cardock.controller;

import com.cardock.dto.CarRequest;
import com.cardock.dto.CarResponse;
import com.cardock.entity.FuelType;
import com.cardock.entity.Transmission;
import com.cardock.service.CarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping("/cars")
    public List<CarResponse> search(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) FuelType fuelType,
            @RequestParam(required = false) Transmission transmission) {
        return carService.search(brand, minPrice, maxPrice, fuelType, transmission);
    }

    @GetMapping("/cars/{id}")
    public CarResponse getCar(@PathVariable Long id) {
        return carService.getAvailableCar(id);
    }

    @GetMapping("/admin/cars")
    public List<CarResponse> getAllCars() {
        return carService.getAllCars();
    }

    @PostMapping("/cars")
    public ResponseEntity<CarResponse> create(@Valid @RequestBody CarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carService.create(request));
    }

    @PutMapping("/cars/{id}")
    public CarResponse update(@PathVariable Long id, @Valid @RequestBody CarRequest request) {
        return carService.update(id, request);
    }

    @DeleteMapping("/cars/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
