package vn.longwaits.tonkho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.longwaits.tonkho.entity.Supplier;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, String> {

    Optional<Supplier> findByCode(String code);
}
