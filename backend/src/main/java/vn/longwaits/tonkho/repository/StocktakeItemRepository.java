package vn.longwaits.tonkho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.longwaits.tonkho.entity.StocktakeItem;

import java.util.List;

public interface StocktakeItemRepository extends JpaRepository<StocktakeItem, String> {

    List<StocktakeItem> findByStocktakeId(String stocktakeId);
}
