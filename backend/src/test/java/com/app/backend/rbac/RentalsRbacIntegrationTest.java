package com.app.backend.rbac;

import com.app.backend.entity.Owner;
import com.app.backend.entity.RentalProperty;
import com.app.backend.entity.RentalPropertyStatus;
import com.app.backend.entity.RentalPropertyType;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.repository.PlotRepository;
import com.app.backend.repository.RentalPropertyRepository;
import com.app.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RentalsRbacIntegrationTest {

    private static final String PASS = "secretpass12";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    OwnerRepository ownerRepository;

    @Autowired
    RentalPropertyRepository rentalPropertyRepository;

    @Autowired
    ListingRepository listingRepository;

    @Autowired
    PlotRepository plotRepository;

    @Autowired
    KhayabanRepository khayabanRepository;

    @Autowired
    PhaseRepository phaseRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    private User agent;
    private RentalProperty assigned;
    private RentalProperty unassigned;
    private Owner registryOwner;

    @BeforeEach
    void seed() {
        listingRepository.deleteAll();
        plotRepository.deleteAll();
        khayabanRepository.deleteAll();
        phaseRepository.deleteAll();
        rentalPropertyRepository.deleteAll();
        ownerRepository.deleteAll();
        userRepository.deleteAll();

        User director = userRepository.save(user("Dir", "rent-dir@test.local", UserRole.DIRECTOR));
        agent = userRepository.save(user("Agt", "rent-agt@test.local", UserRole.AGENT));

        registryOwner = new Owner();
        registryOwner.setName("Rental Registry Owner");
        registryOwner.setContactInfo("rentals@realinvestments.local");
        registryOwner.setCnic("22222-2222222-2");
        registryOwner = ownerRepository.save(registryOwner);

        assigned = new RentalProperty();
        assigned.setTitle("Assigned Unit");
        assigned.setType(RentalPropertyType.RESIDENTIAL);
        assigned.setAddress("123 Main St");
        assigned.setRentAmount(new BigDecimal("1500.00"));
        assigned.setStatus(RentalPropertyStatus.AVAILABLE);
        assigned.setOwner(registryOwner);
        assigned.setAssignedAgent(agent);
        assigned.setCreatedBy(director);
        assigned = rentalPropertyRepository.save(assigned);

        unassigned = new RentalProperty();
        unassigned.setTitle("Open Listing");
        unassigned.setType(RentalPropertyType.COMMERCIAL);
        unassigned.setAddress("999 Biz Rd");
        unassigned.setRentAmount(new BigDecimal("3000.00"));
        unassigned.setStatus(RentalPropertyStatus.PENDING);
        unassigned.setOwner(registryOwner);
        unassigned.setAssignedAgent(null);
        unassigned.setCreatedBy(director);
        unassigned = rentalPropertyRepository.save(unassigned);
    }

    private User user(String name, String email, UserRole role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(PASS));
        u.setRole(role);
        return u;
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, PASS)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("accessToken").asText();
    }

    @Test
    void agentSeesOnlyAssignedRentals() throws Exception {
        String token = login("rent-agt@test.local");

        mockMvc.perform(get("/api/v1/rental-properties")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Assigned Unit"));
    }

    @Test
    void agentCannotGetUnassignedRentalById() throws Exception {
        String token = login("rent-agt@test.local");

        mockMvc.perform(get("/api/v1/rental-properties/" + unassigned.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void directorSeesAllRentals() throws Exception {
        String token = login("rent-dir@test.local");

        String body = mockMvc.perform(get("/api/v1/rental-properties")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        assertThat(root.path("data").path("content").size()).isEqualTo(2);
    }
}
