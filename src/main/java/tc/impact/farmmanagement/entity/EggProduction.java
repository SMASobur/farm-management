package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "egg_productions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"animal_id", "date"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EggProduction implements Production {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(nullable = false)
    private LocalDate date;

    /**
     * Number of good (sellable) eggs collected.
     */
    @Column(name = "good_eggs", nullable = false)
    private Integer goodEggs = 0;

    /**
     * Number of cracked/broken eggs (still sellable at lower price or used internally).
     */
    @Column(name = "cracked_eggs")
    private Integer crackedEggs = 0;

    @Column(name = "total_eggs", nullable = false)
    private Integer totalEggs = 0;

    @PrePersist
    @PreUpdate
    protected void calculateTotal() {
        int good    = goodEggs    != null ? goodEggs    : 0;
        int cracked = crackedEggs != null ? crackedEggs : 0;
        this.totalEggs = good + cracked;
    }

    // ---------- Production interface ----------

    @Override
    public Double getQuantity() {
        // Interface returns Double; we return pieces as double for uniform math.
        return totalEggs.doubleValue();
    }

    @Override
    public String getUnit() {
        return "piece";
    }
}