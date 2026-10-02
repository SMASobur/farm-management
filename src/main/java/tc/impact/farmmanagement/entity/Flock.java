package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "flocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Flock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flock_code", unique = true, nullable = false)
    private String flockCode;   // e.g., "Laying-A"

    private String name;        // e.g., "Laying Hens Batch A"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;    // CHICKEN, DUCK, etc.

    @Column(name = "bird_count", nullable = false)
    private Integer birdCount;  // e.g., 40

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlockStatus status;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.status == null) {
            this.status = FlockStatus.ACTIVE;
        }
    }

    // Reuse the same Species enum from Animal
    public enum Species {
        CHICKEN, DUCK, GOOSE, QUAIL, TURKEY, OTHER
    }

    public enum FlockStatus {
        ACTIVE,     // Currently producing
        RETIRED,    // No longer producing (old hens)
        SOLD,       // Sold off
        DISEASED    // Under quarantine
    }
}