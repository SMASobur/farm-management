package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "animals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "tag_number"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "tag_number", nullable = false)
    private String tagNumber;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 20)
    private LifecycleStatus lifecycleStatus;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.lifecycleStatus == null) {
            this.lifecycleStatus = LifecycleStatus.ACTIVE;
        }
    }

    public enum LifecycleStatus {
        ACTIVE,    // being raised normally
        SOLD,      // sold to someone
        DEAD,      // deceased
        CULLED     // removed from herd (retired, transferred, slaughtered for personal use)
    }

    public enum Gender {
        MALE, FEMALE, UNKNOWN
    }

    public boolean isEggProducer() {
        return species != null
                && species.isEggProducer()
                && lifecycleStatus == LifecycleStatus.ACTIVE;
    }



    public boolean isMilkProducer() {
        boolean isMilkSpecies = species != null && species.isMilkProducer();
        return isMilkSpecies
                && gender == Gender.FEMALE
                && lifecycleStatus == LifecycleStatus.ACTIVE;
    }

    public boolean isActive() {
        return lifecycleStatus == LifecycleStatus.ACTIVE;
    }
}