
function request(path, method, data) {
    if (!method) {
        return fetch(path).then(function (response) {
            if (!response.ok) {
                throw new Error("Request failed");
            }
            return response.json();
        });
    }

    return fetch("/api/csrf")
        .then(function (response) {
            if (!response.ok) {
                throw new Error("Could not load security token");
            }
            return response.json();
        })
        .then(function (csrf) {
            var options = {
                method: method,
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json"
                }
            };

            options.headers[csrf.headerName] = csrf.token;

            if (data !== null) {
                options.body = JSON.stringify(data);
            }

            return fetch(path, options);
        })
        .then(function (response) {
            if (response.status === 204) {
                return null;
            }

            return response.json().then(function (body) {
                if (!response.ok) {
                    throw new Error(body.message || "Request failed");
                }
                return body;
            });
        });
}

function showMessage(text) {
    var box = document.getElementById("form-message") ||
        document.getElementById("booking-message") ||
        document.getElementById("page-message");

    if (box) {
        box.textContent = text;
    }
}

function loadCars() {
    var list = document.getElementById("car-list");
    var search = document.getElementById("car-brand");

    if (!list) {
        return;
    }

    var path = "/api/cars";

    if (search && search.value !== "") {
        path += "?brand=" + encodeURIComponent(search.value);
    }

    request(path).then(function (cars) {
        list.innerHTML = "";

        for (var i = 0; i < cars.length; i++) {
            var car = cars[i];
            var card = document.createElement("div");

            card.className = "car-card";
            card.textContent = car.brand + " " + car.model +
                " - ₹" + car.price;

            var link = document.createElement("a");
            link.href = "/car-details.html?id=" + car.id;
            link.textContent = "View details";

            card.appendChild(link);
            list.appendChild(card);
        }

        showMessage(cars.length + " cars found.");
    }).catch(function () {
        showMessage("Could not load cars.");
    });
}

var searchForm = document.getElementById("car-search");

if (searchForm) {
    searchForm.addEventListener("submit", function (event) {
        event.preventDefault();
        loadCars();
    });

    loadCars();
}

var loginForm = document.getElementById("login-form");
var registerForm = document.getElementById("register-form");

function submitAuthForm(form, isLogin) {
    form.addEventListener("submit", function (event) {
        event.preventDefault();

        var data = {
            email: document.getElementById("email").value,
            password: document.getElementById("password").value
        };

        var path = "/api/auth/login";

        if (!isLogin) {
            path = "/api/auth/register";
            data.fullName = document.getElementById("full-name").value;
        }

        request(path, "POST", data)
            .then(function () {
                if (isLogin) {
                    window.location.href = "/";
                } else {
                    showMessage("Account created. Please log in.");
                }
            })
            .catch(function (error) {
                showMessage(error.message);
            });
    });
}

if (loginForm) {
    submitAuthForm(loginForm, true);
}

if (registerForm) {
    submitAuthForm(registerForm, false);
}

var bookingForm = document.getElementById("booking-form");

if (bookingForm) {
    var carId = new URLSearchParams(window.location.search).get("id");

    request("/api/cars/" + carId)
        .then(function (car) {
            var detail = document.getElementById("car-detail");

            if (detail) {
                detail.textContent = car.brand + " " + car.model +
                    " - ₹" + car.price;
            }
        })
        .catch(function () {
            showMessage("Could not load car details.");
        });

    bookingForm.addEventListener("submit", function (event) {
        event.preventDefault();

        var data = {
            carId: Number(carId),
            appointmentAt: document.getElementById("appointment-time").value
        };

        request("/api/bookings", "POST", data)
            .then(function () {
                showMessage("Appointment requested.");
                bookingForm.reset();
            })
            .catch(function (error) {
                showMessage(error.message);
            });
    });
}

var bookingList = document.getElementById("booking-list");

if (bookingList) {
    request("/api/bookings/my")
        .then(function (bookings) {
            for (var i = 0; i < bookings.length; i++) {
                var booking = bookings[i];
                var item = document.createElement("div");

                item.className = "booking-card";
                item.textContent = booking.carName + " - " +
                    booking.status + " - " +
                    new Date(booking.appointmentAt).toLocaleString();

                if (booking.status !== "CANCELLED" &&
                    new Date(booking.appointmentAt) > new Date()) {

                    var button = document.createElement("button");
                    button.textContent = "Cancel appointment";
                    button.type = "button";
                    button.setAttribute("data-id", booking.id);

                    button.addEventListener("click", function () {
                        var id = this.getAttribute("data-id");

                        request("/api/bookings/" + id + "/cancel", "PATCH", null)
                            .then(function () {
                                window.location.reload();
                            })
                            .catch(function () {
                                showMessage("Could not cancel appointment.");
                            });
                    });

                    item.appendChild(button);
                }

                bookingList.appendChild(item);
            }

            if (bookings.length === 0) {
                showMessage("No appointments yet.");
            }
        })
        .catch(function () {
            showMessage("Could not load bookings.");
        });
}
