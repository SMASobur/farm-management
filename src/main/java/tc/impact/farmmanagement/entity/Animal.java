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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    // ---------- Enums ----------

    public enum Species {
        COW,        // গরু
        GOAT,       // ছাগল
        SHEEP,      // ভেড়া
        CHICKEN,    // মুরগি
        DUCK,       // হাঁস
        BUFFALO,    // মহিষ
        OTHER
    }

    public enum AnimalStatus {
        ACTIVE,      // Healthy, producing (milk/eggs) or being raised
        PREGNANT,    // Expecting offspring
        DRY,         // Not producing milk (resting)
        SICK,        // Under treatment
        SOLD,        // Sold
        DEAD         // Deceased
    }

    public enum Gender {
        MALE,
        FEMALE,
        UNKNOWN
    }

    // ---------- Helper: Is this animal a milk producer? ----------

    public boolean isMilkProducer() {
        boolean isMilkSpecies = this.species == Species.COW
                || this.species == Species.GOAT
                || this.species == Species.SHEEP
                || this.species == Species.BUFFALO;
        return isMilkSpecies && this.gender == Gender.FEMALE;
    }
}