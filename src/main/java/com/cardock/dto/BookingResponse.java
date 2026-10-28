package com.cardock.dto;

import com.cardock.entity.Booking;
import com.cardock.entity.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long userId,
        String customerName,
        Long carId,
        String carName,
        LocalDateTime appointmentAt,
        BookingStatus status,
        LocalDateTime createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getUser().getFullName(),
                booking.getCar().getId(),
                booking.getCar().getBrand() + " " + booking.getCar().getModel(),
                booking.getAppointmentAt(),
                booking.getStatus(),
                booking.getCreatedAt());
    }
}
