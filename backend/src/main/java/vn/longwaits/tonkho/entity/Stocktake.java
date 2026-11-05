package vn.longwaits.tonkho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.longwaits.tonkho.common.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Dot kiem ke va doi soat ton kho. Kiem ke lai tao dot moi tro ve dot cu qua parent_id. */
@Getter
@Setter
@Entity
@Table(name = "stocktakes")
public class Stocktake extends BaseEntity {

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "stocktake_date")
    private LocalDate stocktakeDate;

    @Column(name = "count_file_url")
    private String countFileUrl;

    @Column(name = "damage_file_url")
    private String damageFileUrl;

    private String status = Status.ST_PENDING;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
