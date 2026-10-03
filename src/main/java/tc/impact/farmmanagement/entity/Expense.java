package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    private String notes;

    /**
     * Future: link to a Worker entity for salary tracking.
     * Left null for now; will be populated when Worker module is added.
     */
    // private Worker worker;  // uncomment when Worker is built

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.date == null) {
            this.date = LocalDate.now();
        }
    }

    public enum ExpenseCategory {
        FEED,          // খাদ্য
        MEDICINE,      // ঔষধ
        LABOR,         // শ্রমিক (salary, daily wage, etc.)
        UTILITIES,     // ইউটিলিটি
        EQUIPMENT,     // যন্ত্রপাতি
        TRANSPORT,     // পরিবহন
        RENT,          // ভাড়া
        VET,           // পশু চিকিৎসা
        MAINTENANCE,   // মেরামত
        OTHER          // অন্যান্য
    }
}