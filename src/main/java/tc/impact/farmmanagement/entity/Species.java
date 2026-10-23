package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "species",
        uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "code"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Species {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_bn", length = 100)
    private String nameBn;

    @Column(name = "milk_producer", nullable = false)
    private boolean milkProducer = false;

    @Column(name = "egg_producer", nullable = false)
    private boolean eggProducer = false;

    @Column(name = "emoji")
    private String emoji;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "system_default", nullable = false)
    private boolean systemDefault = false;

    private Integer displayOrder = 100;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.displayOrder == null) this.displayOrder = 100;
    }

    @Column(name = "max_daily_yield")
    private Double maxDailyYield;

    public Double getMaxDailyYield() { return maxDailyYield; }
    public void setMaxDailyYield(Double maxDailyYield) { this.maxDailyYield = maxDailyYield; }
}