package com.cardock.repository;

import com.cardock.entity.Car;
import com.cardock.entity.CarStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Car c where c.id = :id")
    Optional<Car> findByIdForUpdate(@Param("id") Long id);

    List<Car> findByStatus(CarStatus status);

    @Query("""
            select c from Car c
            where c.status = com.cardock.entity.CarStatus.AVAILABLE
              and (:brand is null or lower(c.brand) like lower(concat('%', :brand, '%')))
              and (:minimumPrice is null or c.price >= :minimumPrice)
              and (:maximumPrice is null or c.price <= :maximumPrice)
              and (:fuelType is null or c.fuelType = :fuelType)
              and (:transmission is null or c.transmission = :transmission)
            order by c.createdAt desc
            """)
    List<Car> searchAvailable(
            @Param("brand") String brand,
            @Param("minimumPrice") BigDecimal minimumPrice,
            @Param("maximumPrice") BigDecimal maximumPrice,
            @Param("fuelType") com.cardock.entity.FuelType fuelType,
            @Param("transmission") com.cardock.entity.Transmission transmission);
}
