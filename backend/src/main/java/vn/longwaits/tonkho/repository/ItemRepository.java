package vn.longwaits.tonkho.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.longwaits.tonkho.entity.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, String> {

    Optional<Item> findByCode(String code);

    Optional<Item> findByPosCode(String posCode);

    /** Khoa dong mat hang khi cap nhat ton de tranh hai thao tac cung luc ghi de nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> lockById(@Param("id") String id);

    @Query("select distinct i.category from Item i order by i.category")
    List<String> findCategories();
}
