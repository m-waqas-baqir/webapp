package com.app.backend.rbac;

import com.app.backend.entity.Khayaban;
import com.app.backend.entity.Owner;
import com.app.backend.entity.Phase;
import com.app.backend.entity.Plot;
import com.app.backend.entity.PlotStatus;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.repository.PlotRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlotsRbacIntegrationTest {

    private static final String PASS = "secretpass12";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    OwnerRepository ownerRepository;

    @Autowired
    PhaseRepository phaseRepository;

    @Autowired
    KhayabanRepository khayabanRepository;

    @Autowired
    PlotRepository plotRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    private User director;
    private User agent;
    private Plot assignedPlot;
    private Plot unassignedPlot;
    private Owner registryOwner;

    @BeforeEach
    void seed() {
        plotRepository.deleteAll();
        khayabanRepository.deleteAll();
        phaseRepository.deleteAll();
        ownerRepository.deleteAll();
        userRepository.deleteAll();

        director = userRepository.save(user("Dir", "plots-dir@test.local", UserRole.DIRECTOR));
        agent = userRepository.save(user("Agt", "plots-agt@test.local", UserRole.AGENT));

        registryOwner = new Owner();
        registryOwner.setName("Registry Owner");
        registryOwner.setContactInfo("plots@realinvestments.local");
        registryOwner.setCnic("11111-1111111-1");
        registryOwner = ownerRepository.save(registryOwner);

        Phase phase = new Phase();
        phase.setName("Phase 1");
        phase = phaseRepository.save(phase);

        Khayaban kb = new Khayaban();
        kb.setName("Main Boulevard");
        kb.setPhase(phase);
        kb = khayabanRepository.save(kb);

        assignedPlot = new Plot();
        assignedPlot.setPlotNumber("A-1");
        assignedPlot.setSize(new BigDecimal("5.0000"));
        assignedPlot.setPrice(new BigDecimal("100000.00"));
        assignedPlot.setStatus(PlotStatus.AVAILABLE);
        assignedPlot.setPhase(phase);
        assignedPlot.setKhayaban(kb);
        assignedPlot.setOwner(registryOwner);
        assignedPlot.setAssignedAgent(agent);
        assignedPlot.setCreatedBy(director);
        assignedPlot = plotRepository.save(assignedPlot);

        unassignedPlot = new Plot();
        unassignedPlot.setPlotNumber("B-9");
        unassignedPlot.setSize(new BigDecimal("10.0000"));
        unassignedPlot.setPrice(new BigDecimal("200000.00"));
        unassignedPlot.setStatus(PlotStatus.SOLD);
        unassignedPlot.setPhase(phase);
        unassignedPlot.setKhayaban(kb);
        unassignedPlot.setAssignedAgent(null);
        unassignedPlot.setCreatedBy(director);
        unassignedPlot = plotRepository.save(unassignedPlot);
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
    void agentSeesOnlyAssignedPlots() throws Exception {
        String token = login("plots-agt@test.local");

        mockMvc.perform(get("/api/v1/plots")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].plotNumber").value("A-1"));
    }

    @Test
    void agentCannotGetUnassignedPlotById() throws Exception {
        String token = login("plots-agt@test.local");

        mockMvc.perform(get("/api/v1/plots/" + unassignedPlot.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void agentCanCreatePlot() throws Exception {
        String token = login("plots-agt@test.local");
        Long phaseId = phaseRepository.findAll().get(0).getId();
        Long kbId = khayabanRepository.findAll().get(0).getId();

        String json = String.format(
                "{\"plotNumber\":\"X-1\",\"size\":1.0,\"price\":1.00,\"status\":\"AVAILABLE\","
                        + "\"phaseId\":%d,\"khayabanId\":%d}",
                phaseId, kbId);

        mockMvc.perform(post("/api/v1/plots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plotNumber").value("X-1"));
    }

    @Test
    void agentCannotUpdatePlotCreatedByDirector() throws Exception {
        String token = login("plots-agt@test.local");
        Long phaseId = phaseRepository.findAll().get(0).getId();
        Long kbId = khayabanRepository.findAll().get(0).getId();
        String json = String.format(
                "{\"plotNumber\":\"A-1\",\"size\":5.0,\"price\":100000.00,\"status\":\"AVAILABLE\","
                        + "\"phaseId\":%d,\"khayabanId\":%d}",
                phaseId, kbId);

        mockMvc.perform(put("/api/v1/plots/" + assignedPlot.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void directorSeesAllPlots() throws Exception {
        String token = login("plots-dir@test.local");

        String body = mockMvc.perform(get("/api/v1/plots")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        assertThat(root.path("data").path("content").isArray()).isTrue();
        assertThat(root.path("data").path("content").size()).isEqualTo(2);
    }
}
