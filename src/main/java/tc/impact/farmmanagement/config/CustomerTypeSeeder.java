package tc.impact.farmmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tc.impact.farmmanagement.entity.CustomerType;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.repository.CustomerTypeRepository;
import tc.impact.farmmanagement.repository.FarmRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(5) // after FarmSeeder(1), CategorySeeder(2), UnitSeeder(3), SpeciesSeeder(4)
public class CustomerTypeSeeder implements CommandLineRunner {

    private final CustomerTypeRepository customerTypeRepository;
    private final FarmRepository farmRepository;

    @Override
    public void run(String... args) {
        List<Farm> farms = farmRepository.findAll();
        for (Farm farm : farms) {
            seedDefaults(farm);
        }
    }

    private void seedDefaults(Farm farm) {
        List<CustomerType> defaults = List.of(
                ct("HOUSEHOLD",   "Household",   "বাড়ি",       10),
                ct("SHOP",        "Shop",        "দোকান",       20),
                ct("TEA_STALL",   "Tea Stall",   "চায়ের দোকান", 30),
                ct("RESTAURANT",  "Restaurant",  "রেস্টুরেন্ট", 40),
                ct("WHOLESALER",  "Wholesaler",  "পাইকার",      50),
                ct("OTHER",       "Other",       "অন্যান্য",    999)
        );

        for (CustomerType template : defaults) {
            template.setFarm(farm);
            boolean exists = customerTypeRepository.existsByFarmIdAndCode(farm.getId(), template.getCode());
            if (!exists) {
                customerTypeRepository.save(template);
                log.info("Seeded customer type '{}' for farm '{}'", template.getCode(), farm.getCode());
            }
        }
    }

    private CustomerType ct(String code, String name, String nameBn, int order) {
        CustomerType t = new CustomerType();
        t.setCode(code);
        t.setName(name);
        t.setNameBn(nameBn);
        t.setDisplayOrder(order);
        t.setActive(true);
        t.setSystemDefault(true);
        return t;
    }
}