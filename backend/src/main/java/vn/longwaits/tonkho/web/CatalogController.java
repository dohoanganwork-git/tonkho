package vn.longwaits.tonkho.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.entity.Item;
import vn.longwaits.tonkho.entity.Supplier;
import vn.longwaits.tonkho.service.CatalogService;
import vn.longwaits.tonkho.web.dto.Dtos.ItemRequest;
import vn.longwaits.tonkho.web.dto.Dtos.SupplierRequest;

import java.util.List;

/** Danh muc: moi vai tro duoc xem, chi Quan ly duoc them / sua. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/items")
    public List<Item> items(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) Boolean lowStock,
                            @RequestParam(required = false) Boolean active) {
        return catalogService.items(q, category, lowStock, active);
    }

    @GetMapping("/items/categories")
    public List<String> categories() {
        return catalogService.categories();
    }

    @GetMapping("/items/{id}")
    public Item item(@PathVariable String id) {
        return catalogService.item(id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/items")
    public Item createItem(@Valid @RequestBody ItemRequest req) {
        return catalogService.createItem(req);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/items/{id}")
    public Item updateItem(@PathVariable String id, @Valid @RequestBody ItemRequest req) {
        return catalogService.updateItem(id, req);
    }

    @GetMapping("/suppliers")
    public List<Supplier> suppliers(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Boolean active) {
        return catalogService.suppliers(q, active);
    }

    @GetMapping("/suppliers/{id}")
    public Supplier supplier(@PathVariable String id) {
        return catalogService.supplier(id);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/suppliers")
    public Supplier createSupplier(@Valid @RequestBody SupplierRequest req) {
        return catalogService.createSupplier(req);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/suppliers/{id}")
    public Supplier updateSupplier(@PathVariable String id, @Valid @RequestBody SupplierRequest req) {
        return catalogService.updateSupplier(id, req);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/suppliers/{id}/contract-file")
    public Supplier uploadContract(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        return catalogService.uploadContract(id, file);
    }
}
