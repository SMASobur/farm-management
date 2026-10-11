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
    @Column(nullable = false)
    private AnimalStatus status;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.status == null) {
            this.status = AnimalStatus.ACTIVE;
        }
    }

    public enum AnimalStatus {
        ACTIVE, PREGNANT, DRY, SICK, SOLD, DEAD
    }

    public enum Gender {
        MALE, FEMALE, UNKNOWN
    }

    /**
     * Milk producer status is now determined by the species.
     */
    public boolean isMilkProducer() {
        if (species == null || !species.isMilkProducer()) return false;
        // Only female milk-producing species produce milk
        return this.gender == Gender.FEMALE;
    }
}