package vn.longwaits.tonkho.web;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.security.CurrentUser;
import vn.longwaits.tonkho.service.SalesService;
import vn.longwaits.tonkho.web.dto.Dtos.SalesImportResult;
import vn.longwaits.tonkho.web.dto.Dtos.SalesImportSummary;
import vn.longwaits.tonkho.web.dto.Dtos.SalesLineView;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @GetMapping("/imports")
    public List<SalesImportSummary> imports() {
        return salesService.imports();
    }

    @GetMapping("/imports/{date}")
    public List<SalesLineView> lines(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return salesService.lines(date);
    }

    /** Quan ly nhap file doanh so POS cuoi ngay. */
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/import")
    public SalesImportResult importFile(@RequestParam("salesDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate,
                                        @RequestParam("file") MultipartFile file) {
        return salesService.importFile(salesDate, file, CurrentUser.get());
    }
}
