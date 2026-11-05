package vn.longwaits.tonkho.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.security.CurrentUser;
import vn.longwaits.tonkho.service.StocktakeService;
import vn.longwaits.tonkho.web.dto.Dtos.ReasonRequest;
import vn.longwaits.tonkho.web.dto.Dtos.StocktakeDetail;
import vn.longwaits.tonkho.web.dto.Dtos.StocktakeView;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stocktakes")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;

    @GetMapping
    public List<StocktakeView> list(@RequestParam(required = false) String status) {
        return stocktakeService.list(status);
    }

    @GetMapping("/{id}")
    public StocktakeDetail detail(@PathVariable String id) {
        return stocktakeService.detail(id);
    }

    /** Nhan vien (hoac Quan ly) tai file ton thuc te va file hong/huy. */
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    @PostMapping
    public StocktakeDetail upload(@RequestParam("stocktakeDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @RequestParam("countFile") MultipartFile countFile,
                                  @RequestParam(value = "damageFile", required = false) MultipartFile damageFile,
                                  @RequestParam(value = "parentId", required = false) String parentId) {
        return stocktakeService.upload(date, countFile, damageFile, parentId, CurrentUser.get());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/{id}/approve")
    public StocktakeDetail approve(@PathVariable String id, @Valid @RequestBody ReasonRequest req) {
        return stocktakeService.approve(id, req.reason(), CurrentUser.get());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/{id}/recount")
    public StocktakeDetail recount(@PathVariable String id, @Valid @RequestBody ReasonRequest req) {
        return stocktakeService.requestRecount(id, req.reason(), CurrentUser.get());
    }
}
