package tc.impact.farmmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.Species;
import tc.impact.farmmanagement.repository.FarmRepository;
import tc.impact.farmmanagement.repository.SpeciesRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(4) // after FarmSeeder(1), CategorySeeder(2), UnitSeeder(3)
public class SpeciesSeeder implements CommandLineRunner {

    private final SpeciesRepository speciesRepository;
    private final FarmRepository farmRepository;

    @Override
    public void run(String... args) {
        List<Farm> farms = farmRepository.findAll();
        for (Farm farm : farms) {
            seedDefaults(farm);
        }
    }

    private void seedDefaults(Farm farm) {
        // code, name, nameBn, milkProducer, displayOrder
        List<Species> defaults = List.of(
                sp("COW",     "Cow",     "গরু",     true,  false, 10),
                sp("GOAT",    "Goat",    "ছাগল",    true,  false, 20),
                sp("SHEEP",   "Sheep",   "ভেড়া",    true,  false, 30),
                sp("BUFFALO", "Buffalo", "মহিষ",    true,  false, 40),
                sp("CHICKEN", "Chicken", "মুরগি",   false, true,  50),
                sp("DUCK",    "Duck",    "হাঁস",     false, true,  60),
                sp("GOOSE",   "Goose",   "রাজহাঁস",  false, true,  70),
                sp("OTHER",   "Other",   "অন্যান্য", false, false, 999)
        );


        for (Species template : defaults) {
            template.setFarm(farm);
            boolean exists = speciesRepository.existsByFarmIdAndCode(farm.getId(), template.getCode());
            if (!exists) {
                speciesRepository.save(template);
                log.info("Seeded species '{}' for farm '{}'", template.getCode(), farm.getCode());
            }
        }
    }

    private Species sp(String code, String name, String nameBn, boolean milkProducer, boolean eggProducer, int order) {
        Species s = new Species();
        s.setCode(code);
        s.setName(name);
        s.setNameBn(nameBn);
        s.setMilkProducer(milkProducer);
        s.setEggProducer(eggProducer);
        s.setDisplayOrder(order);
        s.setActive(true);
        s.setSystemDefault(true);
        return s;
    }
}