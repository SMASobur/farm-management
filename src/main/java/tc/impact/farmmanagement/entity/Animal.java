package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "animals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_number", unique = true, nullable = false)
    private String tagNumber;  // e.g., "Cow-01", "Goat-03", "Chicken-12"

    private String name;       // Optional nickname, e.g., "Lakshmi"

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
        return this.species == Species.COW
                || this.species == Species.GOAT
                || this.species == Species.SHEEP
                || this.species == Species.BUFFALO;
    }
}