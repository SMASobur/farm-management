package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Unit unit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Derived from payments. Cached for query performance.
     * Recomputed by the service whenever a payment is added/removed.
     */
    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        calculateAmounts();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateAmounts();
    }

    public void calculateAmounts() {
        if (quantity == null || unitPrice == null) {
            this.totalAmount = BigDecimal.ZERO;
        } else {
            this.totalAmount = quantity.multiply(unitPrice).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        if (paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        this.dueAmount = this.totalAmount.subtract(this.paidAmount);
        if (this.dueAmount.signum() < 0) {
            this.dueAmount = BigDecimal.ZERO; // overpayment handled separately
        }
    }

    // ---------- Enums ----------

    public enum ProductType {
        MILK,     // দুধ
        EGG,      // ডিম
        ANIMAL,   // পশু
        MANURE,   // গোবর
        FEED,     // খাদ্য (surplus sales)
        OTHER
    }

    public enum Unit {
        LITER,    // লিটার
        PIECE,    // পিস
        KG,       // কেজি
        HEAD      // পশুর সংখ্যা
    }
}