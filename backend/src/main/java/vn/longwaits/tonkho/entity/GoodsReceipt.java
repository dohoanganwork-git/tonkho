package vn.longwaits.tonkho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.longwaits.tonkho.common.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Phieu nhap hang, kiem luon cong no va trang thai doi soat cong no. */
@Getter
@Setter
@Entity
@Table(name = "goods_receipts")
public class GoodsReceipt extends BaseEntity {

    private String code;

    @Column(name = "supplier_id")
    private String supplierId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "invoice_no")
    private String invoiceNo;

    @Column(name = "invoice_file_url")
    private String invoiceFileUrl;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "debt_status")
    private String debtStatus = Status.DEBT_UNPAID;

    private String status = Status.ACTIVE;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "reconcile_status")
    private String reconcileStatus = Status.RECON_PENDING;

    @Column(name = "reconcile_note")
    private String reconcileNote;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Con no (cot remaining_amount trong CSDL la cot tu tinh). */
    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(paidAmount);
    }

    public boolean isCancelled() {
        return Status.CANCELLED.equals(status);
    }

    public boolean isReconciled() {
        return Status.RECON_DONE.equals(reconcileStatus);
    }

    /** Cap nhat trang thai cong no theo so da tra. */
    public void refreshDebtStatus() {
        if (paidAmount.signum() <= 0) {
            debtStatus = Status.DEBT_UNPAID;
        } else if (paidAmount.compareTo(totalAmount) >= 0) {
            debtStatus = Status.DEBT_PAID;
        } else {
            debtStatus = Status.DEBT_PARTIAL;
        }
    }
}
