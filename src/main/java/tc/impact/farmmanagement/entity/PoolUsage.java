package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "pool_usage",
        uniqueConstraints = @UniqueConstraint(
                // Today: one row per pool per day
                // Future: drop this constraint to allow multiple entries per day
                columnNames = {"farm_id", "pool_species_id", "pool_product_type", "date"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoolUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /**
     * The species this pool belongs to (Cow, Buffalo, Duck, etc.)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_species_id", nullable = false)
    private Species poolSpecies;

    /**
     * The product type (MILK, EGG, FISH, CROP)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pool_product_type", nullable = false, length = 20)
    private PoolProductType poolProductType;

    @Column(nullable = false)
    private LocalDate date;

    /**
     * Consumed at home / by owner
     * Liters for milk pools, pieces for egg pools
     */
    @Column(name = "internal_use")
    private Double internalUse = 0.0;

    /**
     * Fed to calves, dogs, or other animals
     */
    @Column(name = "fed_to_animals")
    private Double fedToAnimals = 0.0;

    /**
     * Spoiled, spilled, or otherwise unusable
     */
    private Double wasted = 0.0;

    /**
     * Optional note — for future multi-entry mode ("morning", "evening", etc.)
     */
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by_user_id")
    private User enteredBy;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.internalUse == null) this.internalUse = 0.0;
        if (this.fedToAnimals == null) this.fedToAnimals = 0.0;
        if (this.wasted == null) this.wasted = 0.0;
    }
}