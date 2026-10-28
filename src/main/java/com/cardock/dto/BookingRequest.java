package com.cardock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record BookingRequest(
        @NotNull @Positive Long carId,
        @NotNull LocalDateTime appointmentAt) {
}
