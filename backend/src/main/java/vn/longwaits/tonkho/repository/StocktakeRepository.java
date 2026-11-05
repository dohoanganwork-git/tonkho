package vn.longwaits.tonkho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.longwaits.tonkho.entity.Stocktake;

import java.util.List;

public interface StocktakeRepository extends JpaRepository<Stocktake, String> {

    List<Stocktake> findAllByOrderByCreatedAtDesc();

    boolean existsByParentId(String parentId);

    boolean existsByStatus(String status);

    long countByStatus(String status);
}
