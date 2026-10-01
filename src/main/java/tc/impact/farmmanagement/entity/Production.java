package tc.impact.farmmanagement.entity;

import java.time.LocalDate;

/**
 * Common contract for any farm production record (milk, eggs, etc.).
 * Enables unified reporting and analytics across production types.
 */
public interface Production {

    Long getId();

    Animal getAnimal();

    LocalDate getDate();

    /**
     * The quantity produced (liters for milk, pieces for eggs, etc.)
     */
    Double getQuantity();

    /**
     * The unit of measurement ("liter", "piece", etc.)
     */
    String getUnit();
}