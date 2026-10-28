package com.cardock;

import com.cardock.entity.AppUser;
import com.cardock.entity.Booking;
import com.cardock.entity.BookingStatus;
import com.cardock.entity.Car;
import com.cardock.entity.CarStatus;
import com.cardock.entity.FuelType;
import com.cardock.entity.Transmission;
import com.cardock.entity.UserRole;
import com.cardock.repository.BookingRepository;
import com.cardock.repository.CarRepository;
import com.cardock.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CarDockIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CarRepository carRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser customer;
    private AppUser otherCustomer;
    private Car car;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();
        carRepository.deleteAll();

        customer = userRepository.save(new AppUser(
                "Casey Customer", "casey@example.com", passwordEncoder.encode("customer-password"),
                UserRole.CUSTOMER));
        otherCustomer = userRepository.save(new AppUser(
                "Taylor Customer", "taylor@example.com", passwordEncoder.encode("customer-password"),
                UserRole.CUSTOMER));
        car = carRepository.save(new Car(
                "Toyota", "Corolla", LocalDateTime.now().getYear() - 2, new BigDecimal("15000.00"),
                FuelType.PETROL, Transmission.AUTOMATIC, 12000, "Clean car", CarStatus.AVAILABLE));
    }

    @Test
    void registrationCreatesCustomerAndStoresPasswordHash() throws Exception {
        Csrf csrf = getCsrf();
        mockMvc.perform(withCsrf(post("/api/auth/register"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fullName", "  New Customer ",
                                "email", " NEW@Example.com ",
                                "password", "secure-password"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        AppUser saved = userRepository.findByEmail("new@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("New Customer", saved.getFullName());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("secure-password", saved.getPassword()));
    }

    @Test
    void registrationRejectsDuplicateEmailWithConflict() throws Exception {
        Csrf csrf = getCsrf();
        mockMvc.perform(withCsrf(post("/api/auth/register"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fullName", "Casey Duplicate",
                                "email", " CASEY@example.com ",
                                "password", "secure-password"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void registrationRejectsPasswordsBeyondBcryptByteLimit() throws Exception {
        Csrf csrf = getCsrf();
        mockMvc.perform(withCsrf(post("/api/auth/register"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fullName", "Long Password",
                                "email", "long-password@example.com",
                                "password", "é".repeat(37)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginCreatesSessionAndMeReturnsSafeProfile() throws Exception {
        Csrf csrf = getCsrf();
        MvcResult login = mockMvc.perform(withCsrf(post("/api/auth/login"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", customer.getEmail(), "password", "customer-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(customer.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer.getId()))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void invalidLoginReturnsUnauthorized() throws Exception {
        Csrf csrf = getCsrf();
        mockMvc.perform(withCsrf(post("/api/auth/login"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", customer.getEmail(), "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void publicCarSearchSupportsBrandAndPriceFilters() throws Exception {
        mockMvc.perform(get("/api/cars").param("brand", "toy").param("minPrice", "10000").param("maxPrice", "20000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand").value("Toyota"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        mockMvc.perform(get("/api/cars").param("minPrice", "20000").param("maxPrice", "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/cars"));
    }

    @Test
    void publicListingsExcludeReservedCars() throws Exception {
        car.updateDetails(
                car.getBrand(), car.getModel(), car.getManufacturingYear(), car.getPrice(),
                car.getFuelType(), car.getTransmission(), car.getMileageKm(), car.getDescription(),
                CarStatus.RESERVED);
        carRepository.saveAndFlush(car);

        mockMvc.perform(get("/api/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/cars/{id}", car.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void administratorCanViewAllCarsButCustomerCannot() throws Exception {
        car.updateDetails(
                car.getBrand(), car.getModel(), car.getManufacturingYear(), car.getPrice(),
                car.getFuelType(), car.getTransmission(), car.getMileageKm(), car.getDescription(),
                CarStatus.RESERVED);
        carRepository.saveAndFlush(car);

        mockMvc.perform(get("/api/admin/cars").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RESERVED"));

        mockMvc.perform(get("/api/admin/cars").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCarRequestReturnsFieldValidationError() throws Exception {
        Csrf csrf = getCsrf();
        mockMvc.perform(withCsrf(post("/api/cars"), csrf)
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"brand":" ","model":"","manufacturingYear":1890,"price":0,
                                 "fuelType":"PETROL","transmission":"MANUAL","mileageKm":-1,
                                 "status":"AVAILABLE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void customerCannotCreateCarsButAdministratorCan() throws Exception {
        Csrf csrf = getCsrf();
        String request = json(Map.of(
                "brand", "Honda", "model", "Civic",
                "manufacturingYear", LocalDateTime.now().getYear() - 1,
                "price", 18000, "fuelType", "PETROL", "transmission", "MANUAL",
                "mileageKm", 20000, "status", "AVAILABLE"));

        mockMvc.perform(withCsrf(post("/api/cars"), csrf).with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());

        mockMvc.perform(withCsrf(post("/api/cars"), csrf).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.brand").value("Honda"));
    }

    @Test
    void unauthenticatedBookingRequestReturns401() throws Exception {
        Csrf csrf = getCsrf();
        mockMvc.perform(withCsrf(post("/api/bookings"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("carId", car.getId(), "appointmentAt", futureAppointment()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCanBookAndCancelOwnFutureAppointment() throws Exception {
        MockHttpSession session = login(customer.getEmail());
        Csrf csrf = getCsrf();
        MvcResult created = mockMvc.perform(withCsrf(post("/api/bookings"), csrf).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("carId", car.getId(), "appointmentAt", futureAppointment()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.userId").value(customer.getId()))
                .andReturn();

        long bookingId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(withCsrf(patch("/api/bookings/{id}/cancel", bookingId), csrf).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void pastAppointmentIsRejected() throws Exception {
        MockHttpSession session = login(customer.getEmail());
        Csrf csrf = getCsrf();
        mockMvc.perform(withCsrf(post("/api/bookings"), csrf).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("carId", car.getId(), "appointmentAt",
                                LocalDateTime.now().minusDays(1).withNano(0).toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("future")));
    }

    @Test
    void duplicateActiveAppointmentIsRejectedButCancelledSlotCanBeRebooked() throws Exception {
        LocalDateTime appointment = LocalDateTime.now().plusDays(3).withSecond(0).withNano(0);
        Booking existing = bookingRepository.save(new Booking(customer, car, appointment, BookingStatus.PENDING));
        MockHttpSession session = login(otherCustomer.getEmail());
        Csrf csrf = getCsrf();

        mockMvc.perform(withCsrf(post("/api/bookings"), csrf).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("carId", car.getId(), "appointmentAt", appointment.toString()))))
                .andExpect(status().isConflict());

        existing.updateStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(existing);
        mockMvc.perform(withCsrf(post("/api/bookings"), csrf).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("carId", car.getId(), "appointmentAt", appointment.toString()))))
                .andExpect(status().isCreated());
    }

    @Test
    void customerCannotReadAnotherCustomersBooking() throws Exception {
        Booking booking = bookingRepository.save(new Booking(
                customer, car, LocalDateTime.now().plusDays(4), BookingStatus.PENDING));
        MockHttpSession session = login(otherCustomer.getEmail());

        mockMvc.perform(get("/api/bookings/{id}", booking.getId()).session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanListAndUpdateBookingStatus() throws Exception {
        Booking booking = bookingRepository.save(new Booking(
                customer, car, LocalDateTime.now().plusDays(5), BookingStatus.PENDING));
        Csrf csrf = getCsrf();

        mockMvc.perform(get("/api/admin/bookings").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(booking.getId()));

        mockMvc.perform(withCsrf(patch("/api/admin/bookings/{id}/status", booking.getId()), csrf)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void administratorCannotReactivateDuplicateAppointment() throws Exception {
        LocalDateTime appointment = LocalDateTime.now().plusDays(5).withSecond(0).withNano(0);
        bookingRepository.save(new Booking(customer, car, appointment, BookingStatus.PENDING));
        Booking cancelled = bookingRepository.save(
                new Booking(otherCustomer, car, appointment, BookingStatus.CANCELLED));
        Csrf csrf = getCsrf();

        mockMvc.perform(withCsrf(patch("/api/admin/bookings/{id}/status", cancelled.getId()), csrf)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isConflict());
    }

    @Test
    void validationErrorUsesConsistentShape() throws Exception {
        Csrf csrf = getCsrf();
        mockMvc.perform(withCsrf(post("/api/auth/register"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    private MockHttpSession login(String email) throws Exception {
        Csrf csrf = getCsrf();
        MvcResult result = mockMvc.perform(withCsrf(post("/api/auth/login"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "customer-password"))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Csrf getCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        return new Csrf(cookie, response.get("headerName").asText(), response.get("token").asText());
    }

    private MockHttpServletRequestBuilder withCsrf(
            MockHttpServletRequestBuilder request, Csrf csrf) {
        return request.cookie(csrf.cookie())
                .header(csrf.headerName(), csrf.token());
    }

    private String futureAppointment() {
        return LocalDateTime.now().plusDays(2).withSecond(0).withNano(0).toString();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private record Csrf(Cookie cookie, String headerName, String token) {
    }
}
