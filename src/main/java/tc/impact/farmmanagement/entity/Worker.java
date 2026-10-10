package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "workers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "phone"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String address;

    /**
     * Free-text role description: "Cow Herder", "Milker", "Driver", etc.
     * Not an enum roles vary wildly per farm.
     * Custom role management can be added in Settings later.
     */
    @Column(length = 100)
    private String role;

    /**
     * Monthly salary in BDT. Null if daily-wage worker.
     */
    @Column(name = "monthly_salary", precision = 12, scale = 2)
    private BigDecimal monthlySalary;

    /**
     * Daily wage in BDT. Null if salaried worker.
     */
    @Column(name = "daily_wage", precision = 12, scale = 2)
    private BigDecimal dailyWage;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkerStatus status = WorkerStatus.ACTIVE;

    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.status == null) this.status = WorkerStatus.ACTIVE;
    }

    public enum WorkerStatus {
        ACTIVE,
        ON_LEAVE,
        INACTIVE,
        TERMINATED
    }
}