package vn.longwaits.tonkho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Nha cung cap, kem thong tin hop dong (chi luu tru de tra cuu). */
@Getter
@Setter
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    private String code;
    private String name;

    @Column(name = "contact_name")
    private String contactName;

    private String phone;
    private String email;
    private String address;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "contract_no")
    private String contractNo;

    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    @Column(name = "contract_note")
    private String contractNote;

    @Column(name = "contract_file_url")
    private String contractFileUrl;

    @Column(name = "is_active")
    private boolean active = true;
}
