package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "flocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "flock_code"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Flock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "flock_code", nullable = false)
    private String flockCode;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @Column(name = "bird_count", nullable = false)
    private Integer birdCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
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

    public enum FlockStatus {
        ACTIVE,
        RETIRED,
        SOLD,
        DISEASED
    }
}