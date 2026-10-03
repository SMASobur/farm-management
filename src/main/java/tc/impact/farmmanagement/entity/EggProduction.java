package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "egg_productions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EggProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flock_id")
    private Flock flock;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "good_eggs", nullable = false)
    private Integer goodEggs = 0;

    @Column(name = "cracked_eggs", nullable = false)
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

    // ---------- Convenience helpers ----------

    public boolean hasAnimal() { return animal != null; }
    public boolean hasFlock()  { return flock  != null;  }

    /**
     * Display name regardless of source type.
     */
    public String getSourceDisplayName() {
        if (animal != null) return animal.getTagNumber() + " (Animal)";
        if (flock != null)  return flock.getFlockCode() + " (Flock)";
        return "Unknown";
    }

    public Long getSourceId() {
        if (animal != null) return animal.getId();
        if (flock != null)  return flock.getId();
        return null;
    }

    public String getSourceType() {
        if (animal != null) return "ANIMAL";
        if (flock != null)  return "FLOCK";
        return "NONE";
    }
}