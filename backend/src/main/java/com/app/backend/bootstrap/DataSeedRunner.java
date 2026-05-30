package com.app.backend.bootstrap;

import com.app.backend.entity.Khayaban;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Owner;
import com.app.backend.entity.Phase;
import com.app.backend.entity.Plot;
import com.app.backend.entity.PlotStatus;
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
import com.app.backend.security.PermissionCodes;
import com.app.backend.service.security.PermissionAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Idempotent dev/demo seed (skipped when users already exist or when disabled).
 */
@Slf4j
@Component
@Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
@Profile("!test")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataSeedRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhaseRepository phaseRepository;
    private final KhayabanRepository khayabanRepository;
    private final PlotRepository plotRepository;
    private final ListingRepository listingRepository;
    private final OwnerRepository ownerRepository;
    private final RentalPropertyRepository rentalPropertyRepository;
    private final PermissionAssignmentService permissionAssignmentService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.debug("Skipping data seed: users already present");
            return;
        }

        User director = saveUser("Real Investments — Director", "director@seed.local", UserRole.DIRECTOR);
        User admin = saveUser("Real Investments — Admin", "admin@seed.local", UserRole.ADMIN);
        User agent = saveUser("Real Investments — Agent", "agent@seed.local", UserRole.AGENT);

        permissionAssignmentService.replacePermissions(director.getId(), Set.of(
                PermissionCodes.OWNER_VIEW,
                PermissionCodes.OWNER_EDIT,
                PermissionCodes.PLOT_DELETE,
                PermissionCodes.USER_MANAGE,
                PermissionCodes.ASSIGN_AGENT,
                PermissionCodes.VIEW_ACTIVITY_LOGS));
        permissionAssignmentService.replacePermissions(admin.getId(), Set.of(
                PermissionCodes.OWNER_VIEW,
                PermissionCodes.OWNER_EDIT,
                PermissionCodes.PLOT_DELETE,
                PermissionCodes.VIEW_ACTIVITY_LOGS));

        Phase phase = new Phase();
        phase.setName("Real Investments Phase I");
        phase = phaseRepository.save(phase);

        Khayaban kb = new Khayaban();
        kb.setName("Boulevard Central");
        kb.setPhase(phase);
        kb = khayabanRepository.save(kb);

        Owner registryOwner = new Owner();
        registryOwner.setName("Real Investments Sample Owner");
        registryOwner.setContactInfo("+92-300-REAL-INV");
        registryOwner.setCnic("RI-SAMPLE-CNIC-001");
        registryOwner = ownerRepository.save(registryOwner);

        Plot plot = new Plot();
        plot.setPlotNumber("S-101");
        plot.setSize(new BigDecimal("10.0000"));
        plot.setPrice(new BigDecimal("250000.00"));
        plot.setStatus(PlotStatus.AVAILABLE);
        plot.setPhase(phase);
        plot.setKhayaban(kb);
        plot.setOwner(registryOwner);
        plot.setAssignedAgent(agent);
        plot.setCreatedBy(director);
        plotRepository.save(plot);

        Listing listing = new Listing();
        listing.setTitle("Real Investments listing — downtown unit");
        listing.setDescription("Demonstration listing for Real Investments seed data.");
        listing.setOwnerName("Real Investments — listing contact");
        listing.setOwnerEmail("listings@realinvestments.local");
        listing.setAssignedAgentId(agent.getId());
        listingRepository.save(listing);

        RentalProperty rental = new RentalProperty();
        rental.setTitle("Real Investments rental — retail bay");
        rental.setType(RentalPropertyType.COMMERCIAL);
        rental.setAddress("99 Real Investments Commercial Ave");
        rental.setRentAmount(new BigDecimal("45000.00"));
        rental.setStatus(RentalPropertyStatus.AVAILABLE);
        rental.setOwner(registryOwner);
        rental.setAssignedAgent(agent);
        rental.setCreatedBy(admin);
        rentalPropertyRepository.save(rental);

        log.info(
                "Applied data seed: director={} admin={} agent={} (password for all: ChangeMeSeed#2026)",
                director.getEmail(),
                admin.getEmail(),
                agent.getEmail()
        );
    }

    private User saveUser(String name, String email, UserRole role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("ChangeMeSeed#2026"));
        u.setRole(role);
        u.setActive(true);
        return userRepository.save(u);
    }
}
