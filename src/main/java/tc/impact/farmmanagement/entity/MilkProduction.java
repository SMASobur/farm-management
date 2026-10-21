package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "milk_productions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "animal_id", "date"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilkProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by_user_id")
    private User enteredBy;

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

    @Column(name = "liters_sold")
    private Double litersSold = 0.0;

    @Column(name = "liters_internal")
    private Double litersInternal = 0.0;

    @Column(name = "liters_fed")
    private Double litersFed = 0.0;

    @Column(name = "liters_wasted")
    private Double litersWasted = 0.0;

    // Helper: how much is still available for selling
    // = total - (sold + internal + fed + wasted)
    public Double getAvailableForSale() {
        return (totalLiters != null ? totalLiters : 0.0) - getTotalUsage();
    }

    // Helper: total usage = sold + internal + fed + wasted
    public Double getTotalUsage() {
        double s = litersSold != null ? litersSold : 0.0;
        double i = litersInternal != null ? litersInternal : 0.0;
        double f = litersFed != null ? litersFed : 0.0;
        double w = litersWasted != null ? litersWasted : 0.0;
        return s + i + f + w;
    }
}