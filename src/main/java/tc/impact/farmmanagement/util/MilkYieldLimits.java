package tc.impact.farmmanagement.util;

import tc.impact.farmmanagement.entity.Animal;

public class MilkYieldLimits {

    /**
     * Returns the maximum realistic daily milk yield in Liters for a given species.
     */
    public static double getMaxDailyYield(Animal animal) {
        if (animal.getSpecies() == null) return 20.0;

        String speciesCode = animal.getSpecies().getCode().toUpperCase();
        String speciesName = animal.getSpecies().getName().toLowerCase();

        // Cows / Buffaloes: High yield (up to 60L for high-performance dairy)
        if (speciesCode.equals("COW") || speciesCode.equals("BUFFALO") ||
                speciesName.contains("cow") || speciesName.contains("buffalo")) {
            return 60.0;
        }

        // Goats: Up to 8L
        if (speciesCode.equals("GOAT") || speciesName.contains("goat")) {
            return 8.0;
        }

        // Sheep: Up to 5L (very generous)
        if (speciesCode.equals("SHEEP") || speciesName.contains("sheep")) {
            return 5.0;
        }

        // Default fallback for other species (Camel, etc.)
        return 20.0;
    }
}