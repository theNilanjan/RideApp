const state = {
    token: localStorage.getItem("rideapp.token") || "",
    user: JSON.parse(localStorage.getItem("rideapp.user") || "null"),
    lastRide: null
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

function formData(form) {
    return Object.fromEntries(new FormData(form).entries());
}

function coordinatesFromRideForm() {
    const values = formData($("#rideForm"));
    return {
        pickup: {
            latitude: Number(values.pickupLat),
            longitude: Number(values.pickupLng)
        },
        dropoff: {
            latitude: Number(values.dropLat),
            longitude: Number(values.dropLng)
        }
    };
}

async function api(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };
    if (state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }

    const response = await fetch(path, {
        ...options,
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        const message = data?.message || data?.error || `Request failed with ${response.status}`;
        throw new Error(message);
    }
    return data;
}

function saveSession(auth) {
    state.token = auth.token;
    state.user = {
        userId: auth.userId,
        name: auth.name,
        email: auth.email,
        role: auth.role
    };
    localStorage.setItem("rideapp.token", state.token);
    localStorage.setItem("rideapp.user", JSON.stringify(state.user));
    renderSession();
}

function clearSession() {
    state.token = "";
    state.user = null;
    state.lastRide = null;
    localStorage.removeItem("rideapp.token");
    localStorage.removeItem("rideapp.user");
    renderSession();
    $("#ridesList").innerHTML = '<p class="empty">Log in to see rides.</p>';
}

function renderSession() {
    const pulse = $(".pulse");
    if (state.user) {
        $("#sessionName").textContent = state.user.name;
        $("#sessionMeta").textContent = `${state.user.role} | ${state.user.email}`;
        $("#logoutBtn").classList.remove("hidden");
        pulse.classList.add("online");
    } else {
        $("#sessionName").textContent = "Guest session";
        $("#sessionMeta").textContent = "Register or log in to start.";
        $("#logoutBtn").classList.add("hidden");
        pulse.classList.remove("online");
    }
}

function log(title, detail) {
    const entry = document.createElement("div");
    entry.className = "activity-entry";
    entry.innerHTML = `<strong>${title}</strong><span>${detail}</span>`;
    $("#activityLog").prepend(entry);
}

function money(value) {
    if (value === null || value === undefined || value === "") return "--";
    return `₹${Number(value).toFixed(2)}`;
}

function shortId(id) {
    return id ? id.slice(0, 8) : "pending";
}

function rideMarkup(ride) {
    const isDriver = state.user?.role === "ROLE_DRIVER";
    const isRider = state.user?.role === "ROLE_RIDER";
    const actions = [];
    if (isDriver && ride.status === "DRIVER_ASSIGNED") {
        actions.push(`<button data-action="accept" data-id="${ride.rideId}" type="button">Accept</button>`);
        actions.push(`<button data-action="reject" data-id="${ride.rideId}" type="button">Reject</button>`);
    }
    if (isDriver && ["ACCEPTED", "ARRIVED", "IN_PROGRESS"].includes(ride.status)) {
        actions.push(`<button data-action="arrived" data-id="${ride.rideId}" type="button">Arrived</button>`);
        actions.push(`<button data-action="start" data-id="${ride.rideId}" data-otp="${ride.otp || ""}" type="button">Start</button>`);
        actions.push(`<button data-action="complete" data-id="${ride.rideId}" type="button">Complete</button>`);
    }
    if (isRider && ["REQUESTED", "ACCEPTED"].includes(ride.status)) {
        actions.push(`<button data-action="cancel" data-id="${ride.rideId}" type="button">Cancel</button>`);
    }
    if (isRider && ride.status === "COMPLETED") {
        actions.push(`<button data-action="pay" data-id="${ride.rideId}" type="button">Pay</button>`);
        actions.push(`<button data-action="rate" data-id="${ride.rideId}" type="button">Rate</button>`);
    }

    return `
        <article class="ride-item">
            <div class="ride-head">
                <div>
                    <strong>Ride ${shortId(ride.rideId)}</strong>
                    <div class="ride-id">${ride.rideId}</div>
                </div>
                <span class="pill">${ride.status}</span>
            </div>
            <div class="result-row compact">
                <div><span>Fare</span><strong>${money(ride.fare)}</strong></div>
                <div><span>OTP</span><strong>${ride.otp || "--"}</strong></div>
                <div><span>Driver</span><strong>${shortId(ride.driverId)}</strong></div>
            </div>
            ${actions.length ? `<div class="ride-actions">${actions.join("")}</div>` : ""}
        </article>
    `;
}

async function refreshRides() {
    if (!state.user) {
        $("#ridesList").innerHTML = '<p class="empty">Log in to see rides.</p>';
        return;
    }

    const path = state.user.role === "DRIVER" ? "/api/v1/drivers/me/rides" : "/api/v1/rides/me";
    const rides = await api(path);
    $("#ridesList").innerHTML = rides.length
        ? rides.map(rideMarkup).join("")
        : '<p class="empty">No rides yet.</p>';
    log("Rides refreshed", `${rides.length} ride record${rides.length === 1 ? "" : "s"} loaded.`);
}

async function updateDriverSummary() {
    if (state.user?.role !== "ROLE_DRIVER") return;
    const [profile, earnings] = await Promise.all([
        api("/api/v1/drivers/me"),
        api("/api/v1/drivers/me/earnings")
    ]);
    $("#driverRating").textContent = Number(profile.rating || 0).toFixed(1);
    $("#earningsValue").textContent = money(earnings.totalEarnings);
    $("#completedValue").textContent = earnings.completedRides;
}

async function handleRideAction(button) {
    const id = button.dataset.id;
    const action = button.dataset.action;
    if (action === "accept" || action === "reject") {
        await api(`/api/v1/drivers/rides/${id}/decision`, {
            method: "POST",
            body: {accept: action === "accept"}
        });
    }
    if (action === "arrived") {
        await api(`/api/v1/drivers/rides/${id}/status`, {method: "PATCH", body: {status: "ARRIVED"}});
    }
    if (action === "start") {
        const otp = button.dataset.otp || prompt("Enter ride OTP");
        await api(`/api/v1/drivers/rides/${id}/status`, {method: "PATCH", body: {status: "IN_PROGRESS", otp}});
    }
    if (action === "complete") {
        await api(`/api/v1/drivers/rides/${id}/status`, {method: "PATCH", body: {status: "COMPLETED"}});
    }
    if (action === "cancel") {
        await api(`/api/v1/rides/${id}/cancel`, {method: "PATCH", body: {reason: "Cancelled from frontend"}});
    }
    if (action === "pay") {
        await api(`/api/v1/rides/${id}/payments`, {method: "POST", body: {method: "CARD"}});
    }
    if (action === "rate") {
        await api(`/api/v1/rides/${id}/ratings`, {method: "POST", body: {score: 5, comment: "Smooth ride"}});
    }
    log("Ride updated", `${action} completed for ride ${shortId(id)}.`);
    await refreshRides();
    await updateDriverSummary();
}

function bindAuthTabs() {
    $$("[data-auth-tab]").forEach((button) => {
        button.addEventListener("click", () => {
            $$("[data-auth-tab]").forEach((item) => item.classList.remove("active"));
            button.classList.add("active");
            $$(".auth-form").forEach((form) => form.classList.add("hidden"));
            $(`#${button.dataset.authTab}Form`).classList.remove("hidden");
        });
    });
}

function bindForms() {
    $("#loginForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const auth = await api("/api/v1/auth/login", {method: "POST", body: formData(event.currentTarget)});
        saveSession(auth);
        log("Logged in", `${auth.name} is signed in as ${auth.role}.`);
        await refreshRides();
        await updateDriverSummary();
    });

    $("#riderForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const auth = await api("/api/v1/auth/register", {method: "POST", body: formData(event.currentTarget)});
        saveSession(auth);
        log("Rider created", `${auth.name} can now book rides.`);
    });

    $("#driverForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const auth = await api("/api/v1/auth/drivers/register", {method: "POST", body: formData(event.currentTarget)});
        saveSession(auth);
        log("Driver created", `${auth.name} can now receive rides.`);
        await updateDriverSummary();
    });

    $("#estimateBtn").addEventListener("click", async () => {
        const estimate = await api("/api/v1/rides/estimate", {method: "POST", body: coordinatesFromRideForm()});
        $("#fareValue").textContent = money(estimate.estimatedFare);
        $("#distanceValue").textContent = `${Number(estimate.distanceKm).toFixed(2)} km`;
        $("#surgeValue").textContent = `${Number(estimate.surgeMultiplier).toFixed(2)}x`;
        log("Fare estimated", `${money(estimate.estimatedFare)} for ${Number(estimate.distanceKm).toFixed(2)} km.`);
    });

    $("#rideForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const ride = await api("/api/v1/rides", {method: "POST", body: coordinatesFromRideForm()});
        state.lastRide = ride;
        log("Ride booked", `Ride ${shortId(ride.rideId)} is ${ride.status}.`);
        await refreshRides();
    });

    $("#driverLocationForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const values = formData(event.currentTarget);
        const profile = await api("/api/v1/drivers/me/location", {
            method: "PATCH",
            body: {
                latitude: Number(values.latitude),
                longitude: Number(values.longitude),
                available: event.currentTarget.available.checked
            }
        });
        $("#driverRating").textContent = Number(profile.rating || 0).toFixed(1);
        log("Driver updated", profile.available ? "Driver is available for rides." : "Driver is offline.");
        await updateDriverSummary();
    });

    $("#refreshRidesBtn").addEventListener("click", () => refreshRides().catch(showError));
    $("#clearLogBtn").addEventListener("click", () => $("#activityLog").innerHTML = "");
    $("#logoutBtn").addEventListener("click", clearSession);
    $("#ridesList").addEventListener("click", (event) => {
        const button = event.target.closest("[data-action]");
        if (button) {
            handleRideAction(button).catch(showError);
        }
    });
}

function showError(error) {
    log("Request failed", error.message);
}

window.addEventListener("unhandledrejection", (event) => {
    event.preventDefault();
    showError(event.reason);
});

bindAuthTabs();
bindForms();
renderSession();
if (state.user) {
    refreshRides().catch(showError);
    updateDriverSummary().catch(showError);
}
