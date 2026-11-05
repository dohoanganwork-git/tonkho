package vn.longwaits.tonkho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.longwaits.tonkho.common.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(name = "receipt_id")
    private String receiptId;

    @Column(name = "created_by")
    private String createdBy;

    private BigDecimal amount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    private String method;

    @Column(name = "evidence_file_url")
    private String evidenceFileUrl;

    private String status = Status.ACTIVE;

    private String note;
}
