package tc.impact.farmmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.Unit;
import tc.impact.farmmanagement.repository.FarmRepository;
import tc.impact.farmmanagement.repository.UnitRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(3) // after FarmSeeder(1), CategorySeeder(2)
public class UnitSeeder implements CommandLineRunner {

    private final UnitRepository unitRepository;
    private final FarmRepository farmRepository;

    @Override
    public void run(String... args) {
        List<Farm> farms = farmRepository.findAll();
        for (Farm farm : farms) {
            seedDefaults(farm);
        }
    }

    private void seedDefaults(Farm farm) {
        List<Unit> defaults = List.of(
                unit("LITER", "Liter",     "লিটার",    "L",  10),
                unit("PIECE", "Piece",     "পিস",      "pcs", 20),
                unit("KG",    "Kilogram",  "কেজি",     "kg",  30),
                unit("HEAD",  "Head",      "মাথা",     "h",   40),
                unit("MANA",  "Mana (40kg)", "মণ",     "মণ",  50),
                unit("TON",   "Ton",       "টন",       "t",   60),
                unit("DOZEN", "Dozen",     "ডজন",      "dzn", 70)
        );

        for (Unit template : defaults) {
            template.setFarm(farm);
            boolean exists = unitRepository.existsByFarmIdAndCode(farm.getId(), template.getCode());
            if (!exists) {
                unitRepository.save(template);
                log.info("Seeded unit '{}' for farm '{}'", template.getCode(), farm.getCode());
            }
        }
    }

    private Unit unit(String code, String name, String nameBn, String abbrev, int order) {
        Unit u = new Unit();
        u.setCode(code);
        u.setName(name);
        u.setNameBn(nameBn);
        u.setAbbreviation(abbrev);
        u.setDisplayOrder(order);
        u.setActive(true);
        u.setSystemDefault(true);
        return u;
    }
}