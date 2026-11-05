package vn.longwaits.tonkho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.longwaits.tonkho.entity.SalesLine;

import java.time.LocalDate;
import java.util.List;

public interface SalesLineRepository extends JpaRepository<SalesLine, String> {

    boolean existsBySalesDate(LocalDate salesDate);

    List<SalesLine> findBySalesDate(LocalDate salesDate);

    List<SalesLine> findBySalesDateBetween(LocalDate from, LocalDate to);

    List<SalesLine> findAllByOrderBySalesDateDesc();

    List<SalesLine> findBySalesDateGreaterThanEqualAndOverQuantityGreaterThan(LocalDate from, int over);
}
