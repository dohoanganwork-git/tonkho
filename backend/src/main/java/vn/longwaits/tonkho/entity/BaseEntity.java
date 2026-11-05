package vn.longwaits.tonkho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Lop cha cho moi bang: khoa chinh la UUID dang chuoi 36 ky tu. */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @PrePersist
    protected void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
