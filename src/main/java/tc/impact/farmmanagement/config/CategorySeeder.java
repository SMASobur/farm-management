package tc.impact.farmmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tc.impact.farmmanagement.entity.Category;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.repository.CategoryRepository;
import tc.impact.farmmanagement.repository.FarmRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // runs after FarmSeeder (@Order(1))
public class CategorySeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final FarmRepository farmRepository;

    @Override
    public void run(String... args) {
        List<Farm> farms = farmRepository.findAll();
        if (farms.isEmpty()) {
            log.warn("No farms found. CategorySeeder skipped.");
            return;
        }
        for (Farm farm : farms) {
            seedExpenseCategories(farm);
            seedProductCategories(farm);
        }
    }

    private void seedExpenseCategories(Farm farm) {
        List<Category> defaults = List.of(
                seed("FEED",        "Feed",         "খাদ্য",        10),
                seed("MEDICINE",    "Medicine",     "ঔষধ",          20),
                seed("VACCINE",     "Vaccine",      "টিকা",         30),
                seed("VET",         "Vet Service",  "পশু চিকিৎসা",   40),
                seed("SALARY",      "Salary",       "বেতন",         50),
                seed("LABOR",       "Labor",        "শ্রমিক",       60),
                seed("UTILITIES",   "Utilities",    "ইউটিলিটি",     70),
                seed("TRANSPORT",   "Transport",    "পরিবহন",       80),
                seed("FUEL",        "Fuel",         "জ্বালানি",     90),
                seed("RENT",        "Rent",         "ভাড়া",         100),
                seed("MAINTENANCE", "Maintenance",  "মেরামত",       110),
                seed("PACKAGING",   "Packaging",    "প্যাকেজিং",    120),
                seed("EQUIPMENT",   "Equipment",    "যন্ত্রপাতি",    130),
                seed("OTHER",       "Other",        "অন্যান্য",     999)
        );
        insertMissing(defaults, farm, Category.CategoryType.EXPENSE);
    }

    private void seedProductCategories(Farm farm) {
        List<Category> defaults = List.of(
                seed("MILK",    "Milk",     "দুধ",      10),
                seed("EGG",     "Egg",      "ডিম",      20),
                seed("ANIMAL",  "Animal",   "পশু",      30),
                seed("MANURE",  "Manure",   "গোবর",     40),
                seed("FEED",    "Feed",     "খাদ্য",    50),
                seed("OTHER",   "Other",    "অন্যান্য", 999)
        );
        insertMissing(defaults, farm, Category.CategoryType.PRODUCT);
    }

    private Category seed(String code, String name, String nameBn, int order) {
        Category c = new Category();
        c.setCode(code);
        c.setName(name);
        c.setNameBn(nameBn);
        c.setDisplayOrder(order);
        c.setActive(true);
        c.setSystemDefault(true);
        return c;
    }

    private void insertMissing(List<Category> defaults, Farm farm, Category.CategoryType type) {
        for (Category template : defaults) {
            template.setFarm(farm);
            template.setType(type);
            boolean exists = categoryRepository.existsByFarmIdAndCodeAndType(farm.getId(), template.getCode(), type);
            if (!exists) {
                categoryRepository.save(template);
                log.info("Seeded {} category for farm '{}': {} ({})",
                        type, farm.getCode(), template.getCode(), template.getName());
            }
        }
    }
}