package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "milk_productions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"animal_id", "date"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilkProduction implements Production {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "morning_liters")
    private Double morningLiters = 0.0;

    @Column(name = "noon_liters")
    private Double noonLiters = 0.0;

    @Column(name = "night_liters")
    private Double nightLiters = 0.0;

    @Column(name = "total_liters", nullable = false)
    private Double totalLiters = 0.0;

    @PrePersist
    @PreUpdate
    protected void calculateTotal() {
        double morning = morningLiters != null ? morningLiters : 0.0;
        double noon    = noonLiters    != null ? noonLiters    : 0.0;
        double night   = nightLiters   != null ? nightLiters   : 0.0;
        this.totalLiters = morning + noon + night;
    }

    // ---------- Production interface ----------

    @Override
    public Double getQuantity() {
        return totalLiters;
    }

    @Override
    public String getUnit() {
        return "liter";
    }
}