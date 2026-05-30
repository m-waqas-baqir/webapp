package com.app.backend.rbac;

import com.app.backend.entity.Listing;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.repository.ListingRepository;
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

import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListingRbacIntegrationTest {

    private static final String PASS = "secretpass12";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ListingRepository listingRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    private User director;
    private User admin;
    private User agent;
    private Listing assignedListing;

    @BeforeEach
    void seed() {
        listingRepository.deleteAll();
        userRepository.deleteAll();

        director = userRepository.save(user("Director One", "rbac-dir@test.local", UserRole.DIRECTOR));
        admin = userRepository.save(user("Admin One", "rbac-adm@test.local", UserRole.ADMIN));
        agent = userRepository.save(user("Agent One", "rbac-agt@test.local", UserRole.AGENT));

        assignedListing = new Listing();
        assignedListing.setTitle("Plot A");
        assignedListing.setDescription("Details");
        assignedListing.setOwnerName("Jane Owner");
        assignedListing.setOwnerEmail("listings@realinvestments.local");
        assignedListing.setAssignedAgentId(agent.getId());
        assignedListing = listingRepository.save(assignedListing);

        Listing unassigned = new Listing();
        unassigned.setTitle("Plot B");
        unassigned.setDescription("Other");
        unassigned.setOwnerName("Bob Owner");
        unassigned.setOwnerEmail("properties@realinvestments.local");
        unassigned.setAssignedAgentId(null);
        listingRepository.save(unassigned);
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
                        .content(String.format(
                                "{\"email\":\"%s\",\"password\":\"%s\"}",
                                email, PASS)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("accessToken").asText();
    }

    @Test
    void agentSeesOnlyAssignedListingsAndNoOwnerPii() throws Exception {
        String token = login("rbac-agt@test.local");

        mockMvc.perform(get("/api/v1/listings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Plot A"))
                .andExpect(jsonPath("$.data[0].ownerName").doesNotExist())
                .andExpect(jsonPath("$.data[0].ownerEmail").doesNotExist())
                .andExpect(jsonPath("$.data[0].ownerVisibilitySummary").doesNotExist());
    }

    @Test
    void agentCannotAccessUnassignedListingById() throws Exception {
        String token = login("rbac-agt@test.local");
        Long otherId = listingRepository.findAll().stream()
                .filter(l -> "Plot B".equals(l.getTitle()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/v1/listings/" + otherId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void agentCannotCreateListing() throws Exception {
        String token = login("rbac-agt@test.local");

        mockMvc.perform(post("/api/v1/listings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"description\":\"\",\"ownerName\":\"O\",\"ownerEmail\":\"o@o.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSeesMaskedOwnerAndCanListAll() throws Exception {
        String token = login("rbac-adm@test.local");

        String json = mockMvc.perform(get("/api/v1/listings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(json).path("data");
        assertThat(data.isArray()).isTrue();
        for (JsonNode row : data) {
            assertThat(row.has("ownerVisibilitySummary")).isTrue();
            assertThat(row.has("ownerEmail")).isFalse();
        }
    }

    @Test
    void directorReportForbiddenForAdmin() throws Exception {
        String token = login("rbac-adm@test.local");

        mockMvc.perform(get("/api/v1/reports/listings/owners")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void directorReportShowsOwnerEmails() throws Exception {
        String token = login("rbac-dir@test.local");

        String json = mockMvc.perform(get("/api/v1/reports/listings/owners")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(json).path("data");
        long jane = StreamSupport.stream(data.spliterator(), false)
                .filter(n -> "listings@realinvestments.local".equals(n.path("ownerEmail").asText()))
                .count();
        long bob = StreamSupport.stream(data.spliterator(), false)
                .filter(n -> "properties@realinvestments.local".equals(n.path("ownerEmail").asText()))
                .count();
        assertThat(jane).isEqualTo(1);
        assertThat(bob).isEqualTo(1);
    }

    @Test
    void directorListingIncludesRawOwnerFields() throws Exception {
        String token = login("rbac-dir@test.local");

        mockMvc.perform(get("/api/v1/listings/" + assignedListing.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerEmail").value("listings@realinvestments.local"))
                .andExpect(jsonPath("$.data.ownerName").value("Jane Owner"));
    }

    @Test
    void invalidAssignedAgentIdReturns400() throws Exception {
        String token = login("rbac-adm@test.local");

        mockMvc.perform(post("/api/v1/listings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"title\":\"Z\",\"description\":\"\",\"ownerName\":\"O\",\"ownerEmail\":\"z@z.com\","
                                        + "\"assignedAgentId\":%d}",
                                director.getId())))
                .andExpect(status().isBadRequest());

        assertThat(listingRepository.findAll().stream().noneMatch(l -> "Z".equals(l.getTitle()))).isTrue();
    }
}
