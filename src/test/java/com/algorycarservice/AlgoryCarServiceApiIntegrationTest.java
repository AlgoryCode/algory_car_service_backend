package com.algorycarservice;

import com.algorycarservice.common.storage.InMemoryPhotoStorage;
import com.algorycarservice.leave.model.LeaveDays;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole context shares one in-memory database, so the first admin is registered once and reused. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlgoryCarServiceApiIntegrationTest {

    private static final String ADMIN_EMAIL = "yonetici@algorycarservice.test";
    private static final String PASSWORD = "guclu-sifre-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryPhotoStorage storage;

    private String admin;

    @BeforeEach
    void signInAdmin() throws Exception {
        String setup = mockMvc.perform(get("/api/v1/auth/setup-status")).andReturn().getResponse().getContentAsString();
        if (JsonPath.<Boolean>read(setup, "$.setupRequired")) {
            mockMvc.perform(json(post("/api/v1/auth/register"), """
                            {"fullName": "Ayşe Yönetici", "email": "%s", "password": "%s"}
                            """.formatted(ADMIN_EMAIL, PASSWORD)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.role").value("ADMIN"));
        }
        admin = login(ADMIN_EMAIL);
    }

    @Test
    void registrationClosesAfterFirstAdmin() throws Exception {
        mockMvc.perform(json(post("/api/v1/auth/register"), """
                        {"fullName": "Başka Biri", "email": "baska@algorycarservice.test", "password": "%s"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_CLOSED"));
    }

    @Test
    void staffInheritAdminsBusinessUuid() throws Exception {
        String me = mockMvc.perform(authorized(get("/api/v1/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessUuid").isString())
                .andReturn().getResponse().getContentAsString();
        String businessUuid = JsonPath.read(me, "$.businessUuid");

        String staff = login(createStaff());
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessUuid").value(businessUuid));
    }

    @Test
    void staffCannotSeeFinanceOrManageStaff() throws Exception {
        String staff = login(createStaff());

        mockMvc.perform(get("/api/v1/finance/summary").param("year", "2026").header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(json(post("/api/v1/staff"), staffJson("x@algorycarservice.test")).header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard").header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthIncome").doesNotExist());
    }

    @Test
    void collectingWorkOrderPaymentBooksIncome() throws Exception {
        String plate = "34 TS " + (100 + (int) (Math.random() * 899));
        String vehicle = mockMvc.perform(authorized(json(post("/api/v1/vehicles"), """
                        {"plate": "%s", "brand": "Fiat", "model": "Egea", "ownerName": "Mehmet Demir", "ownerPhone": "05321234567"}
                        """.formatted(plate))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate").value(plate.replace(" ", "")))
                .andReturn().getResponse().getContentAsString();
        int vehicleId = JsonPath.read(vehicle, "$.id");

        String today = LocalDate.now().toString();
        String order = mockMvc.perform(authorized(json(post("/api/v1/work-orders"), """
                        {"vehicleId": %d, "status": "OPEN", "complaint": "Periyodik bakım", "mileageKm": 45000,
                         "openedOn": "%s", "items": [
                           {"kind": "PART", "description": "Yağ filtresi", "quantity": 1, "unitPrice": 350,
                            "part": "ENGINE", "operation": "SERVICED"},
                           {"kind": "LABOR", "description": "İşçilik", "quantity": 1.5, "unitPrice": 1000, "operation": "PAINTED"}
                         ]}
                        """.formatted(vehicleId, today))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(1850.0))
                .andExpect(jsonPath("$.items[0].part").value("ENGINE"))
                .andExpect(jsonPath("$.items[0].operation").value("SERVICED"))
                .andExpect(jsonPath("$.items[1].operation").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        int orderId = JsonPath.read(order, "$.id");

        mockMvc.perform(authorized(json(put("/api/v1/work-orders/" + orderId + "/status"), """
                        {"status": "COMPLETED"}
                        """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedOn").value(today));

        mockMvc.perform(authorized(json(post("/api/v1/work-orders/" + orderId + "/payment"), """
                        {"paidOn": "%s"}
                        """.formatted(today))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidOn").value(today));

        String transactions = mockMvc.perform(authorized(get("/api/v1/finance/transactions")
                        .param("year", String.valueOf(LocalDate.now().getYear()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer linkedId = JsonPath.<List<Integer>>read(transactions, "$[?(@.workOrderId == %d)].id".formatted(orderId)).getFirst();

        mockMvc.perform(authorized(delete("/api/v1/finance/transactions/" + linkedId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LINKED_TRANSACTION"));

        mockMvc.perform(authorized(get("/api/v1/vehicles/" + vehicleId + "/work-orders")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId));

        mockMvc.perform(authorized(get("/api/v1/vehicles/by-plate/" + plate.toLowerCase(Locale.ROOT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicleId));
        mockMvc.perform(authorized(get("/api/v1/vehicles/by-plate/00XX000")))
                .andExpect(status().isNotFound());

        mockMvc.perform(authorized(get("/api/v1/finance/summary").param("year", String.valueOf(LocalDate.now().getYear()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeByCategory[*].category", hasItem("SERVICE")));
    }

    @Test
    void leaveRequestIsApprovedAndShownOnCalendar() throws Exception {
        String staffEmail = createStaff();
        String staff = login(staffEmail);
        LocalDate monday = LocalDate.now().plusWeeks(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        String leave = mockMvc.perform(json(post("/api/v1/leaves"), """
                        {"type": "ANNUAL", "startDate": "%s", "endDate": "%s", "reason": "Tatil"}
                        """.formatted(monday, sunday)).header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.days").value(6))
                .andReturn().getResponse().getContentAsString();
        int leaveId = JsonPath.read(leave, "$.id");

        mockMvc.perform(json(post("/api/v1/leaves"), """
                        {"type": "SICK", "startDate": "%s", "endDate": "%s"}
                        """.formatted(monday.plusDays(1), monday.plusDays(1))).header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LEAVE_OVERLAPS"));

        mockMvc.perform(authorized(json(post("/api/v1/leaves/" + leaveId + "/approve"), "{}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(authorized(get("/api/v1/leaves/calendar")
                        .param("year", String.valueOf(monday.getYear()))
                        .param("month", String.valueOf(monday.getMonthValue()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(leaveId)));

        mockMvc.perform(get("/api/v1/leaves/balance").param("year", String.valueOf(monday.getYear()))
                        .header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.used").value(LeaveDays.countInYear(monday, sunday, monday.getYear())))
                .andExpect(jsonPath("$.allowance").value(14));
    }

    @Test
    void marksCarryPhotosAndOnlyTheirAuthorMayRemoveThem() throws Exception {
        String vehicle = mockMvc.perform(authorized(json(post("/api/v1/vehicles"), """
                        {"plate": "06 MK %d", "brand": "Toyota", "model": "Corolla", "ownerName": "Elif Kara"}
                        """.formatted(100 + (int) (Math.random() * 899)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int vehicleId = JsonPath.read(vehicle, "$.id");
        String staff = login(createStaff());

        String mark = mockMvc.perform(json(post("/api/v1/vehicles/" + vehicleId + "/marks"), """
                        {"view": "MODEL", "x": 0.82, "y": 0.4, "z": 0.6, "type": "DENT", "part": "FRONT_LEFT_DOOR", "note": "Kapı göçüğü"}
                        """).header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.part").value("FRONT_LEFT_DOOR"))
                .andExpect(jsonPath("$.z").value(0.6))
                .andExpect(jsonPath("$.createdBy.fullName").value("Can Usta"))
                .andReturn().getResponse().getContentAsString();
        int markId = JsonPath.read(mark, "$.id");

        mockMvc.perform(json(post("/api/v1/vehicles/" + vehicleId + "/marks"), """
                        {"view": "TOP", "x": 1.5, "y": 0.2, "type": "SCRATCH"}
                        """).header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(json(post("/api/v1/vehicles/" + vehicleId + "/marks"), """
                        {"view": "MODEL", "x": 0.5, "y": 0.2, "type": "SCRATCH"}
                        """).header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isBadRequest());

        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1, 1, 0};
        String photo = mockMvc.perform(multipart("/api/v1/vehicle-marks/" + markId + "/photos")
                        .file(new MockMultipartFile("file", "kapi.jpg", "image/jpeg", jpeg))
                        .header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int photoId = JsonPath.read(photo, "$.id");
        assertEquals(1, storage.size());

        mockMvc.perform(multipart("/api/v1/vehicle-marks/" + markId + "/photos")
                        .file(new MockMultipartFile("file", "not.jpg", "image/jpeg", "not an image at all".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PHOTO_INVALID"));

        mockMvc.perform(authorized(get("/api/v1/vehicle-mark-photos/" + photoId)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpeg));
        mockMvc.perform(authorized(get("/api/v1/vehicles/" + vehicleId + "/marks")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].photos[0].id").value(photoId));

        String otherStaff = login(createStaff());
        mockMvc.perform(delete("/api/v1/vehicle-marks/" + markId).header(HttpHeaders.AUTHORIZATION, otherStaff))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/vehicle-marks/" + markId).header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isNoContent());
        mockMvc.perform(authorized(get("/api/v1/vehicle-mark-photos/" + photoId)))
                .andExpect(status().isNotFound());
        assertEquals(0, storage.size(), "the photo file goes with its mark");
    }

    @Test
    void adminBooksInspectionAndOpensWorkOrderOnArrival() throws Exception {
        String plate = "06 RD " + (100 + (int) (Math.random() * 899));
        String vehicle = mockMvc.perform(authorized(json(post("/api/v1/vehicles"), """
                        {"plate": "%s", "brand": "Renault", "model": "Clio", "ownerName": "Zeynep Kaya", "ownerPhone": "05551112233"}
                        """.formatted(plate))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int vehicleId = JsonPath.read(vehicle, "$.id");
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);

        mockMvc.perform(authorized(get("/api/v1/appointments/slots").param("date", monday.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$.capacity").value(2))
                .andExpect(jsonPath("$.slots[0].time").value(startsWith("09:00")))
                .andExpect(jsonPath("$.slots[0].available").value(true));

        String booked = mockMvc.perform(authorized(json(post("/api/v1/appointments"), appointmentJson(vehicleId, plate, monday, "10:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.vehicle.id").value(vehicleId))
                .andExpect(jsonPath("$.endTime").value(startsWith("10:30")))
                .andReturn().getResponse().getContentAsString();
        int appointmentId = JsonPath.read(booked, "$.id");

        mockMvc.perform(authorized(json(post("/api/v1/appointments"), appointmentJson(null, plate, monday, "14:00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_DUPLICATE"));
        mockMvc.perform(authorized(get("/api/v1/appointments").param("from", monday.toString()).param("to", monday.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(appointmentId)));

        String done = mockMvc.perform(authorized(post("/api/v1/appointments/" + appointmentId + "/work-order")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();
        int workOrderId = JsonPath.read(done, "$.workOrderId");
        mockMvc.perform(authorized(get("/api/v1/work-orders/" + workOrderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicle.id").value(vehicleId))
                .andExpect(jsonPath("$.complaint").value(startsWith("Araç muayenesi")));
        mockMvc.perform(authorized(json(put("/api/v1/appointments/" + appointmentId + "/status"), """
                        {"status": "CANCELLED"}
                        """)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_STATUS_INVALID"));
    }

    @Test
    void staffRequestWaitsForAdminAndSlotsFillUp() throws Exception {
        String staff = login(createStaff());
        LocalDate tuesday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY)).plusWeeks(1);

        String request = mockMvc.perform(json(post("/api/v1/appointments"), appointmentJson(null, "35 YK 001", tuesday, "11:00"))
                        .header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.plate").value("35YK001"))
                .andExpect(jsonPath("$.vehicle").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(request, "$.id");

        mockMvc.perform(json(put("/api/v1/appointments/" + id + "/status"), """
                        {"status": "CONFIRMED"}
                        """).header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isForbidden());
        mockMvc.perform(authorized(json(put("/api/v1/appointments/" + id + "/status"), """
                        {"status": "CONFIRMED"}
                        """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(authorized(post("/api/v1/appointments/" + id + "/work-order")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_VEHICLE_REQUIRED"));

        mockMvc.perform(authorized(json(post("/api/v1/appointments"), appointmentJson(null, "35 YK 002", tuesday, "11:00"))))
                .andExpect(status().isCreated());
        mockMvc.perform(authorized(json(post("/api/v1/appointments"), appointmentJson(null, "35 YK 003", tuesday, "11:00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_SLOT_FULL"));
        mockMvc.perform(authorized(get("/api/v1/appointments/slots").param("date", tuesday.toString())))
                .andExpect(jsonPath("$.slots[4].time").value(startsWith("11:00")))
                .andExpect(jsonPath("$.slots[4].booked").value(2))
                .andExpect(jsonPath("$.slots[4].available").value(false));

        mockMvc.perform(authorized(json(post("/api/v1/appointments"), appointmentJson(null, "35 YK 004", tuesday, "11:10"))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_INVALID_TIME"));
        LocalDate sunday = tuesday.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        mockMvc.perform(authorized(json(post("/api/v1/appointments"), appointmentJson(null, "35 YK 005", sunday, "11:00"))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_CLOSED_DAY"));
        mockMvc.perform(delete("/api/v1/appointments/" + id).header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isForbidden());
    }

    private static String appointmentJson(Integer vehicleId, String plate, LocalDate date, String time) {
        return """
                {"vehicleId": %s, "plate": "%s", "customerName": "Ali Veli", "customerPhone": "05320000000",
                 "type": "INSPECTION", "date": "%s", "startTime": "%s"}
                """.formatted(vehicleId, plate, date, time);
    }

    private String createStaff() throws Exception {
        String email = UUID.randomUUID() + "@algorycarservice.test";
        mockMvc.perform(authorized(json(post("/api/v1/staff"), staffJson(email))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"));
        return email;
    }

    private static String staffJson(String email) {
        return """
                {"fullName": "Can Usta", "email": "%s", "password": "%s", "position": "Usta",
                 "role": "STAFF", "annualLeaveDays": 14}
                """.formatted(email, PASSWORD);
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(json(post("/api/v1/auth/login"), """
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(response, "$.accessToken");
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, admin);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
