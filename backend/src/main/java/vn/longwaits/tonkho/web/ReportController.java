package vn.longwaits.tonkho.web;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.longwaits.tonkho.service.ReportService;
import vn.longwaits.tonkho.web.dto.Dtos.AccuracyRow;
import vn.longwaits.tonkho.web.dto.Dtos.AlertView;
import vn.longwaits.tonkho.web.dto.Dtos.ConsumptionReport;
import vn.longwaits.tonkho.web.dto.Dtos.Dashboard;
import vn.longwaits.tonkho.web.dto.Dtos.InventoryReport;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/reports/dashboard")
    public Dashboard dashboard() {
        return reportService.dashboard();
    }

    @GetMapping("/reports/inventory")
    public InventoryReport inventory(@RequestParam(required = false) String category) {
        return reportService.inventory(category);
    }

    @GetMapping("/reports/consumption")
    public ConsumptionReport consumption(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.consumption(from, to);
    }

    @GetMapping("/reports/stocktakes")
    public List<AccuracyRow> stocktakes(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.stocktakeAccuracy(from, to);
    }

    @GetMapping("/alerts")
    public AlertView alerts() {
        return reportService.alerts();
    }
}
