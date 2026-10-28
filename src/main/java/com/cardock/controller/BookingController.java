package com.cardock.controller;

import com.cardock.dto.BookingRequest;
import com.cardock.dto.BookingResponse;
import com.cardock.dto.UpdateBookingStatusRequest;
import com.cardock.entity.BookingStatus;
import com.cardock.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(user.getUsername(), request));
    }

    @GetMapping("/bookings/my")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal UserDetails user) {
        return bookingService.getMyBookings(user.getUsername());
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        boolean administrator = false;
        for (GrantedAuthority authority : user.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                administrator = true;
                break;
            }
        }
        return bookingService.getById(id, user.getUsername(), administrator);
    }

    @PatchMapping("/bookings/{id}/cancel")
    public BookingResponse cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return bookingService.cancel(id, user.getUsername());
    }

    @GetMapping("/admin/bookings")
    public List<BookingResponse> allBookings() {
        return bookingService.getAllBookings();
    }

    @PatchMapping("/admin/bookings/{id}/status")
    public BookingResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        return bookingService.updateStatus(id, request.status());
    }
}
