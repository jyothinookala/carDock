package com.cardock.service;

import com.cardock.dto.BookingRequest;
import com.cardock.dto.BookingResponse;
import com.cardock.entity.AppUser;
import com.cardock.entity.Booking;
import com.cardock.entity.BookingStatus;
import com.cardock.entity.Car;
import com.cardock.entity.CarStatus;
import com.cardock.exception.ConflictException;
import com.cardock.exception.ForbiddenOperationException;
import com.cardock.exception.ResourceNotFoundException;
import com.cardock.repository.BookingRepository;
import com.cardock.repository.CarRepository;
import com.cardock.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, CarRepository carRepository,
                          UserRepository userRepository, AuthService authService, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse create(String email, BookingRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!request.appointmentAt().isAfter(now)) {
            throw new IllegalArgumentException("Appointment must be in the future");
        }

        AppUser user = findUser(email);
        Car car = carRepository.findByIdForUpdate(request.carId())
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));
        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new ConflictException("This car is not currently available for appointments");
        }
        if (bookingRepository.existsByCarIdAndAppointmentAtAndStatusNot(
                car.getId(), request.appointmentAt(), BookingStatus.CANCELLED)) {
            throw new ConflictException("An active appointment already exists for this car and time");
        }

        Booking booking = bookingRepository.save(
                new Booking(user, car, request.appointmentAt(), BookingStatus.PENDING));
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String email) {
        AppUser user = findUser(email);
        List<Booking> bookings = bookingRepository.findByUserIdOrderByAppointmentAtDesc(user.getId());
        List<BookingResponse> responses = new ArrayList<>();
        for (Booking booking : bookings) {
            responses.add(BookingResponse.from(booking));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(Long bookingId, String email, boolean administrator) {
        Booking booking = findBooking(bookingId);
        if (!administrator && !booking.getUser().getEmail().equals(authService.normalizeEmail(email))) {
            throw new ForbiddenOperationException("You can only view your own bookings");
        }
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, String email) {
        Booking booking = findBooking(bookingId);
        if (!booking.getUser().getEmail().equals(authService.normalizeEmail(email))) {
            throw new ForbiddenOperationException("You can only cancel your own bookings");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("This booking is already cancelled");
        }
        if (!booking.getAppointmentAt().isAfter(LocalDateTime.now(clock))) {
            throw new ConflictException("Past appointments cannot be cancelled");
        }
        booking.updateStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAllByOrderByAppointmentAtDesc();
        List<BookingResponse> responses = new ArrayList<>();
        for (Booking booking : bookings) {
            responses.add(BookingResponse.from(booking));
        }
        return responses;
    }

    @Transactional
    public BookingResponse updateStatus(Long bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        carRepository.findByIdForUpdate(booking.getCar().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));
        if (status != BookingStatus.CANCELLED
                && bookingRepository.existsByCarIdAndAppointmentAtAndStatusNotAndIdNot(
                        booking.getCar().getId(), booking.getAppointmentAt(), BookingStatus.CANCELLED, bookingId)) {
            throw new ConflictException("An active appointment already exists for this car and time");
        }
        booking.updateStatus(status);
        return BookingResponse.from(booking);
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmail(authService.normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }
}
