package com.algorycarservice;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** OtoViser: customers find businesses and book them; businesses answer. Shares the context with the workshop tests. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OtoViserMarketIntegrationTest {

    private static final String PASSWORD = "guclu-sifre-1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void customerBooksAndBusinessConfirms() throws Exception {
        String city = "İzmir-" + UUID.randomUUID().toString().substring(0, 6);
        Registered provider = registerProvider("Usta Oto Servis", city, 1);
        String customer = registerCustomer();
        String otherCustomer = registerCustomer();

        // Turkish case rules: "İZMİR" and "izmir" find the same city.
        mockMvc.perform(get("/api/v1/market/businesses").param("category", "REPAIR")
                        .param("city", city.toUpperCase(java.util.Locale.forLanguageTag("tr")))
                        .header(HttpHeaders.AUTHORIZATION, customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Usta Oto Servis"));
        mockMvc.perform(get("/api/v1/market/businesses").param("category", "ROADSIDE").param("city", city)
                        .header(HttpHeaders.AUTHORIZATION, customer))
                .andExpect(jsonPath("$", hasSize(0)));

        LocalDate monday = LocalDate.now(ZoneId.of("Europe/Istanbul")).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        String booking = mockMvc.perform(authorized(customer, json(post("/api/v1/market/bookings"),
                        bookingJson(provider.businessId(), "REPAIR", monday, "10:00", "34 abc 123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.plate").value("34ABC123"))
                .andExpect(jsonPath("$.endTime").value("10:30:00"))
                .andExpect(jsonPath("$.business.name").value("Usta Oto Servis"))
                .andReturn().getResponse().getContentAsString();
        int bookingId = JsonPath.read(booking, "$.id");

        // Capacity 1: the slot is taken for everyone else while the request is open.
        mockMvc.perform(authorized(otherCustomer, json(post("/api/v1/market/bookings"),
                        bookingJson(provider.businessId(), "REPAIR", monday, "10:00", "06 KLM 45"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_SLOT_FULL"));
        mockMvc.perform(authorized(otherCustomer, json(post("/api/v1/market/bookings"),
                        bookingJson(provider.businessId(), "ROADSIDE", monday, "11:00", "06 KLM 45"))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_OFFERED"));
        mockMvc.perform(get("/api/v1/market/businesses/" + provider.businessId() + "/slots").param("date", monday.toString())
                        .header(HttpHeaders.AUTHORIZATION, otherCustomer))
                .andExpect(jsonPath("$.capacity").value(1))
                .andExpect(jsonPath("$.slots[?(@.time == '10:00:00')].available").value(hasItem(false)));

        // The business sees the request with the customer's phone and confirms it.
        mockMvc.perform(get("/api/v1/provider/bookings").header(HttpHeaders.AUTHORIZATION, provider.token()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customer.phone").value("05320000000"));
        mockMvc.perform(authorized(provider.token(), json(put("/api/v1/provider/bookings/" + bookingId + "/status"),
                        """
                        {"status": "CONFIRMED"}
                        """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(authorized(provider.token(), json(put("/api/v1/provider/bookings/" + bookingId + "/status"),
                        """
                        {"status": "REJECTED"}
                        """)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_STATUS_INVALID"));
        mockMvc.perform(get("/api/v1/market/bookings").header(HttpHeaders.AUTHORIZATION, customer))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        // Someone else's booking cannot be cancelled; the owner's cancel frees the slot.
        mockMvc.perform(authorized(otherCustomer, put("/api/v1/market/bookings/" + bookingId + "/cancel")))
                .andExpect(status().isNotFound());
        mockMvc.perform(authorized(customer, json(put("/api/v1/market/bookings/" + bookingId + "/cancel"), """
                        {"note": "Planım değişti"}
                        """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.statusNote").value("Planım değişti"));
        mockMvc.perform(authorized(otherCustomer, json(post("/api/v1/market/bookings"),
                        bookingJson(provider.businessId(), "REPAIR", monday, "10:00", "06 KLM 45"))))
                .andExpect(status().isCreated());
    }

    @Test
    void marketplaceAccountsStayOutOfTheWorkshop() throws Exception {
        String customer = registerCustomer();
        Registered provider = registerProvider("Merkez Oto Elektrik", "Ankara", 2);

        for (String token : new String[]{customer, provider.token()}) {
            mockMvc.perform(get("/api/v1/vehicles").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/dashboard").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/staff").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/provider/business").header(HttpHeaders.AUTHORIZATION, customer))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/market/bookings").header(HttpHeaders.AUTHORIZATION, provider.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/market/businesses").header(HttpHeaders.AUTHORIZATION, provider.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/provider/business").header(HttpHeaders.AUTHORIZATION, provider.token()))
                .andExpect(jsonPath("$.slotCapacity").value(2))
                .andExpect(jsonPath("$.categories", not(hasItem("ROADSIDE"))));
    }

    @Test
    void signUpRejectsTakenEmailAndMissingCategories() throws Exception {
        String email = UUID.randomUUID() + "@otoviser.test";
        mockMvc.perform(json(post("/api/v1/auth/register/customer"), customerJson(email))).andExpect(status().isCreated());
        mockMvc.perform(json(post("/api/v1/auth/register/customer"), customerJson(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        mockMvc.perform(json(post("/api/v1/auth/register/provider"), """
                        {"fullName": "Ali Usta", "email": "%s", "password": "%s",
                         "business": {"name": "Ali Oto", "phone": "02120000000", "city": "İstanbul",
                                      "district": "Kadıköy", "address": "Moda Cd. 1", "categories": []}}
                        """.formatted(UUID.randomUUID() + "@otoviser.test", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private record Registered(String token, int businessId) {
    }

    private Registered registerProvider(String name, String city, int capacity) throws Exception {
        String response = mockMvc.perform(json(post("/api/v1/auth/register/provider"), """
                        {"fullName": "Mehmet Usta", "email": "%s", "password": "%s",
                         "business": {"name": "%s", "phone": "02320000000", "city": "%s", "district": "Bornova",
                                      "address": "Sanayi Sitesi 12. Sk. No: 4", "categories": ["REPAIR", "INSPECTION"],
                                      "slotCapacity": %d}}
                        """.formatted(UUID.randomUUID() + "@otoviser.test", PASSWORD, name, city, capacity)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PROVIDER"))
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + JsonPath.read(response, "$.accessToken");
        String business = mockMvc.perform(get("/api/v1/provider/business").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Registered(token, JsonPath.read(business, "$.id"));
    }

    private String registerCustomer() throws Exception {
        String response = mockMvc.perform(json(post("/api/v1/auth/register/customer"),
                        customerJson(UUID.randomUUID() + "@otoviser.test")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(response, "$.accessToken");
    }

    private static String customerJson(String email) {
        return """
                {"fullName": "Zeynep Kaya", "email": "%s", "phone": "05320000000", "password": "%s"}
                """.formatted(email, PASSWORD);
    }

    private static String bookingJson(int businessId, String category, LocalDate date, String time, String plate) {
        return """
                {"businessId": %d, "category": "%s", "date": "%s", "startTime": "%s", "plate": "%s",
                 "vehicle": "Renault Clio 2019", "note": "Frenden ses geliyor"}
                """.formatted(businessId, category, date, time, plate);
    }

    private static MockHttpServletRequestBuilder authorized(String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, token);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
