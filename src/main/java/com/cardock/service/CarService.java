package com.cardock.service;

import com.cardock.dto.CarRequest;
import com.cardock.dto.CarResponse;
import com.cardock.entity.Car;
import com.cardock.entity.CarStatus;
import com.cardock.entity.FuelType;
import com.cardock.entity.Transmission;
import com.cardock.exception.ConflictException;
import com.cardock.exception.ResourceNotFoundException;
import com.cardock.repository.BookingRepository;
import com.cardock.repository.CarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Service
public class CarService {

    private final CarRepository carRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public CarService(CarRepository carRepository, BookingRepository bookingRepository, Clock clock) {
        this.carRepository = carRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CarResponse> search(
            String brand, BigDecimal minimumPrice, BigDecimal maximumPrice,
            FuelType fuelType, Transmission transmission) {
        if (minimumPrice != null && minimumPrice.signum() < 0
                || maximumPrice != null && maximumPrice.signum() < 0) {
            throw new IllegalArgumentException("Price filters cannot be negative");
        }
        if (minimumPrice != null && maximumPrice != null && minimumPrice.compareTo(maximumPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot exceed maximum price");
        }
        String normalizedBrand = brand == null || brand.isBlank() ? null : brand.trim();
        List<Car> cars = carRepository.searchAvailable(
                normalizedBrand, minimumPrice, maximumPrice, fuelType, transmission);
        List<CarResponse> responses = new ArrayList<>();
        for (Car car : cars) {
            responses.add(CarResponse.from(car));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<CarResponse> getAllCars() {
        List<Car> cars = carRepository.findAll();
        List<CarResponse> responses = new ArrayList<>();
        for (Car car : cars) {
            responses.add(CarResponse.from(car));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public CarResponse getAvailableCar(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Available car not found"));
        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new ResourceNotFoundException("Available car not found");
        }
        return CarResponse.from(car);
    }

    @Transactional
    public CarResponse create(CarRequest request) {
        validateYear(request.manufacturingYear());
        Car car = carRepository.save(toCar(request));
        return CarResponse.from(car);
    }

    @Transactional
    public CarResponse update(Long id, CarRequest request) {
        validateYear(request.manufacturingYear());
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));
        car.updateDetails(
                request.brand().trim(), request.model().trim(), request.manufacturingYear(),
                request.price(), request.fuelType(), request.transmission(), request.mileageKm(),
                request.description(), request.status());
        return CarResponse.from(car);
    }

    @Transactional
    public void delete(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));
        if (bookingRepository.existsByCarId(id)) {
            throw new ConflictException("Cars with booking history cannot be deleted");
        }
        carRepository.delete(car);
    }

    private Car toCar(CarRequest request) {
        return new Car(
                request.brand().trim(), request.model().trim(), request.manufacturingYear(),
                request.price(), request.fuelType(), request.transmission(), request.mileageKm(),
                request.description(), request.status());
    }

    private void validateYear(Integer manufacturingYear) {
        int currentYear = Year.now(clock).getValue();
        if (manufacturingYear < 1900 || manufacturingYear > currentYear) {
            throw new IllegalArgumentException("Manufacturing year must be from 1900 through " + currentYear);
        }
    }

}
