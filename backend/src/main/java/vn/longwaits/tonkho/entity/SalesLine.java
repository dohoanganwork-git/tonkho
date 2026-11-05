package vn.longwaits.tonkho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Mot dong doanh so POS: mot mat hang trong mot ngay (dong vai tro phieu xuat ban). */
@Getter
@Setter
@Entity
@Table(name = "sales_lines")
public class SalesLine extends BaseEntity {

    @Column(name = "imported_by")
    private String importedBy;

    @Column(name = "item_id")
    private String itemId;

    @Column(name = "sales_date")
    private LocalDate salesDate;

    @Column(name = "original_file_url")
    private String originalFileUrl;

    private int quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "stock_before")
    private int stockBefore;

    @Column(name = "stock_after")
    private int stockAfter;

    /** Phan ban vuot qua ton so sach (ton khong duoc am). */
    @Column(name = "over_quantity")
    private int overQuantity;

    @Column(name = "imported_at", updatable = false)
    private LocalDateTime importedAt = LocalDateTime.now();

    public BigDecimal getAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
