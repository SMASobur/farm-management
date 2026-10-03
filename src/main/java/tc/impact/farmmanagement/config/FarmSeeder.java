package tc.impact.farmmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.repository.FarmRepository;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // runs before other seeders
public class FarmSeeder implements CommandLineRunner {

    private final FarmRepository farmRepository;

    @Value("${farm.default.code:DEFAULT}")
    private String defaultCode;

    @Value("${farm.default.name:Default Farm}")
    private String defaultName;

    @Value("${farm.default.owner-name:Administrator}")
    private String defaultOwnerName;

    @Override
    public void run(String... args) {
        if (farmRepository.existsByCode(defaultCode)) {
            log.info("Default farm '{}' already exists — skipping seed", defaultCode);
            return;
        }

        Farm farm = new Farm();
        farm.setCode(defaultCode);
        farm.setName(defaultName);
        farm.setOwnerName(defaultOwnerName);
        farm.setPlan(Farm.SubscriptionPlan.FREE);
        farm.setActive(true);

        Farm saved = farmRepository.save(farm);
        log.info("Seeded default farm: id={}, code={}, name={}",
                saved.getId(), saved.getCode(), saved.getName());
    }
}