package vn.longwaits.tonkho.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.longwaits.tonkho.common.Status;
import vn.longwaits.tonkho.entity.GoodsReceipt;
import vn.longwaits.tonkho.entity.Item;
import vn.longwaits.tonkho.entity.SalesLine;
import vn.longwaits.tonkho.entity.Stocktake;
import vn.longwaits.tonkho.entity.StocktakeItem;
import vn.longwaits.tonkho.repository.GoodsReceiptRepository;
import vn.longwaits.tonkho.repository.ItemRepository;
import vn.longwaits.tonkho.repository.SalesLineRepository;
import vn.longwaits.tonkho.repository.StocktakeItemRepository;
import vn.longwaits.tonkho.repository.StocktakeRepository;
import vn.longwaits.tonkho.security.CurrentUser;
import vn.longwaits.tonkho.web.dto.Dtos.AccuracyRow;
import vn.longwaits.tonkho.web.dto.Dtos.AlertView;
import vn.longwaits.tonkho.web.dto.Dtos.ConsumptionReport;
import vn.longwaits.tonkho.web.dto.Dtos.ConsumptionRow;
import vn.longwaits.tonkho.web.dto.Dtos.Dashboard;
import vn.longwaits.tonkho.web.dto.Dtos.InventoryReport;
import vn.longwaits.tonkho.web.dto.Dtos.InventoryRow;
import vn.longwaits.tonkho.web.dto.Dtos.SalesLineView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bao cao: gia tri ton kho, tieu thu theo POS, ty le chinh xac kho; canh bao ton thap. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ItemRepository itemRepository;
    private final SalesLineRepository salesRepository;
    private final StocktakeRepository stocktakeRepository;
    private final StocktakeItemRepository stocktakeItemRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final SalesService salesService;
    private final Lookups lookups;

    /** Gia tri ton chi hien voi Quan ly va Ke toan. */
    public InventoryReport inventory(String category) {
        boolean showValue = !CurrentUser.isStaff();
        List<InventoryRow> rows = itemRepository.findAll().stream()
                .filter(Item::isActive)
                .filter(i -> category == null || category.isBlank() || category.equals(i.getCategory()))
                .sorted(Comparator.comparing(Item::getCode))
                .map(i -> new InventoryRow(i.getId(), i.getCode(), i.getName(), i.getCategory(), i.getUnit(),
                        i.getCurrentStock(), i.getMinStock(), i.isLowStock(),
                        showValue ? i.getLastCost() : null,
                        showValue ? i.getLastCost().multiply(BigDecimal.valueOf(i.getCurrentStock())) : null))
                .toList();
        int qty = rows.stream().mapToInt(InventoryRow::currentStock).sum();
        BigDecimal value = showValue
                ? rows.stream().map(InventoryRow::stockValue).reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;
        int low = (int) rows.stream().filter(InventoryRow::lowStock).count();
        return new InventoryReport(rows, qty, value, low);
    }

    public ConsumptionReport consumption(LocalDate from, LocalDate to) {
        Map<String, int[]> qty = new LinkedHashMap<>();
        Map<String, BigDecimal> amount = new LinkedHashMap<>();
        for (SalesLine s : salesRepository.findBySalesDateBetween(from, to)) {
            qty.computeIfAbsent(s.getItemId(), k -> new int[1])[0] += s.getQuantity();
            amount.merge(s.getItemId(), s.getAmount(), BigDecimal::add);
        }
        Map<String, Item> items = lookups.items(qty.keySet());
        List<ConsumptionRow> rows = new ArrayList<>();
        qty.forEach((id, q) -> {
            Item i = items.get(id);
            rows.add(new ConsumptionRow(id, i == null ? "" : i.getCode(), i == null ? "" : i.getName(),
                    i == null ? "" : i.getCategory(), i == null ? "" : i.getUnit(), q[0], amount.get(id)));
        });
        rows.sort(Comparator.comparingInt(ConsumptionRow::quantity).reversed());
        int totalQty = rows.stream().mapToInt(ConsumptionRow::quantity).sum();
        BigDecimal totalAmount = rows.stream().map(ConsumptionRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ConsumptionReport(from, to, rows, totalQty, totalAmount);
    }

    public List<AccuracyRow> stocktakeAccuracy(LocalDate from, LocalDate to) {
        List<AccuracyRow> out = new ArrayList<>();
        for (Stocktake s : stocktakeRepository.findAllByOrderByCreatedAtDesc()) {
            if ((from != null && s.getStocktakeDate().isBefore(from)) || (to != null && s.getStocktakeDate().isAfter(to))) {
                continue;
            }
            List<StocktakeItem> lines = stocktakeItemRepository.findByStocktakeId(s.getId());
            int matched = (int) lines.stream().filter(l -> l.getVarianceQty() == 0).count();
            double pct = lines.isEmpty() ? 100.0
                    : BigDecimal.valueOf(matched * 100.0 / lines.size()).setScale(1, RoundingMode.HALF_UP).doubleValue();
            int absVariance = lines.stream().mapToInt(l -> Math.abs(l.getVarianceQty())).sum();
            int damaged = lines.stream().mapToInt(StocktakeItem::getDamagedQty).sum();
            out.add(new AccuracyRow(s.getId(), s.getStocktakeDate(), s.getStatus(), lines.size(), matched, pct,
                    absVariance, damaged));
        }
        return out;
    }

    public Dashboard dashboard() {
        List<Item> items = itemRepository.findAll().stream().filter(Item::isActive).toList();
        int low = (int) items.stream().filter(Item::isLowStock).count();
        BigDecimal debt = BigDecimal.ZERO;
        long unpaid = 0;
        for (GoodsReceipt r : receiptRepository.findAll()) {
            if (!r.isCancelled() && r.getRemainingAmount().signum() > 0) {
                debt = debt.add(r.getRemainingAmount());
                unpaid++;
            }
        }
        LocalDate lastSales = salesRepository.findAllByOrderBySalesDateDesc().stream()
                .findFirst().map(SalesLine::getSalesDate).orElse(null);
        long pending = stocktakeRepository.countByStatus(Status.ST_PENDING);
        boolean staff = CurrentUser.isStaff();
        return new Dashboard(items.size(), low, staff ? null : debt, unpaid, pending, lastSales);
    }

    /** Canh bao: ton thap (moi vai tro) va ban vuot ton trong 7 ngay gan nhat (chi Quan ly). */
    public AlertView alerts() {
        List<Item> low = itemRepository.findAll().stream()
                .filter(i -> i.isActive() && i.isLowStock())
                .sorted(Comparator.comparingInt(Item::getCurrentStock))
                .toList();
        List<SalesLineView> over = List.of();
        if (Status.ROLE_MANAGER.equals(CurrentUser.get().role())) {
            over = salesService.toViews(salesRepository
                    .findBySalesDateGreaterThanEqualAndOverQuantityGreaterThan(LocalDate.now().minusDays(7), 0));
        }
        return new AlertView(low, over);
    }
}
