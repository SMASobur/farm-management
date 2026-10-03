package tc.impact.farmmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.User;
import tc.impact.farmmanagement.repository.FarmRepository;
import tc.impact.farmmanagement.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(3) // runs after FarmSeeder(1) and CategorySeeder(2)
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${farm.default.admin-username:admin}")
    private String adminUsername;

    @Value("${farm.default.admin-password:admin123}")
    private String adminPassword;

    @Value("${farm.default.admin-full-name:Farm Administrator}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        // Only seed if there are NO users at all
        if (userRepository.count() > 0) {
            log.info("Users already exist — AdminSeeder skipped");
            return;
        }

        Farm farm = farmRepository.findAll().stream()
                .findFirst()
                .orElse(null);

        if (farm == null) {
            log.warn("No farm found — AdminSeeder skipped");
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFullName(adminFullName);
        admin.setFarm(farm);
        admin.setRole(User.Role.OWNER);
        admin.setActive(true);

        userRepository.save(admin);
        log.warn("=== DEFAULT ADMIN CREATED: username='{}', password='{}' ===",
                adminUsername, adminPassword);
        log.warn("=== CHANGE THIS PASSWORD IMMEDIATELY IN PRODUCTION ===");
    }
}