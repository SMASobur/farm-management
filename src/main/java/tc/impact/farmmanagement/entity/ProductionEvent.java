package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "production_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /**
     * The animal this event belongs to. Nullable because future
     * fish/crop events will reference pond_id/field_id instead.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    /**
     * Short title, e.g. "Fever", "FMD Vaccine", "Weighed 320kg".
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Optional longer description.
     */
    @Column(length = 1000)
    private String description;

    /**
     * Optional cost — vet fees, medicine, etc.
     */
    @Column(precision = 14, scale = 2)
    private BigDecimal cost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by_user_id")
    private User enteredBy;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.date == null) this.date = LocalDate.now();
    }

    public enum EventType {
        ILLNESS,
        TREATMENT,
        VACCINATION,
        DEWORMING,
        CHECKUP,
        PREGNANCY,
        CALVING,
        WEIGHT,
        PURCHASE,
        SALE,
        DEATH,
        NOTE,
        OTHER
    }
}