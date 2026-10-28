package com.cardock.repository;

import com.cardock.entity.Booking;
import com.cardock.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    List<Booking> findByUserIdOrderByAppointmentAtDesc(Long userId);
    List<Booking> findAllByOrderByAppointmentAtDesc();
    boolean existsByCarIdAndAppointmentAtAndStatusNot(
            Long carId, LocalDateTime appointmentAt, BookingStatus excludedStatus);
    boolean existsByCarIdAndAppointmentAtAndStatusNotAndIdNot(
            Long carId, LocalDateTime appointmentAt, BookingStatus excludedStatus, Long bookingId);
    boolean existsByCarId(Long carId);
}
