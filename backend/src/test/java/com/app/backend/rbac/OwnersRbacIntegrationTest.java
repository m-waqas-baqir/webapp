package com.app.backend.rbac;

import com.app.backend.entity.Owner;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.repository.PlotRepository;
import com.app.backend.repository.RentalPropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnersRbacIntegrationTest {

    private static final String PASS = "secretpass12";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    OwnerRepository ownerRepository;

    @Autowired
    ListingRepository listingRepository;

    @Autowired
    PlotRepository plotRepository;

    @Autowired
    KhayabanRepository khayabanRepository;

    @Autowired
    PhaseRepository phaseRepository;

    @Autowired
    RentalPropertyRepository rentalPropertyRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    private Owner owner;

    @BeforeEach
    void seed() {
        listingRepository.deleteAll();
        plotRepository.deleteAll();
        khayabanRepository.deleteAll();
        phaseRepository.deleteAll();
        rentalPropertyRepository.deleteAll();
        ownerRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(user("Dir", "own-dir@test.local", UserRole.DIRECTOR));
        userRepository.save(user("Adm", "own-adm@test.local", UserRole.ADMIN));
        userRepository.save(user("Agt", "own-agt@test.local", UserRole.AGENT));

        owner = new Owner();
        owner.setName("Real Investments Sample Owner");
        owner.setContactInfo("owners@realinvestments.local");
        owner.setCnic("12345-1234567-3");
        owner = ownerRepository.save(owner);
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
    void directorSeesFullCnicAndContact() throws Exception {
        String token = login("own-dir@test.local");
        mockMvc.perform(get("/api/v1/owners/" + owner.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cnic").value("12345-1234567-3"))
                .andExpect(jsonPath("$.data.contactInfo").value("owners@realinvestments.local"))
                .andExpect(jsonPath("$.data.name").value("Real Investments Sample Owner"));
    }

    @Test
    void adminSeesMaskedFields() throws Exception {
        String token = login("own-adm@test.local");
        mockMvc.perform(get("/api/v1/owners/" + owner.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("R. O."))
                .andExpect(jsonPath("$.data.contactInfo").value("o***@r***.local"))
                .andExpect(jsonPath("$.data.cnic").value("************67-3"));
    }

    @Test
    void agentCannotAccessOwnerRegistry() throws Exception {
        String token = login("own-agt@test.local");
        mockMvc.perform(get("/api/v1/owners/" + owner.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCannotCreateOwner() throws Exception {
        String token = login("own-agt@test.local");
        String body = "{\"name\":\"X\",\"contactInfo\":\"a@b.com\",\"cnic\":\"99999-9999999-9\"}";
        mockMvc.perform(post("/api/v1/owners")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
