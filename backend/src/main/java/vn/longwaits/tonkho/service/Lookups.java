package vn.longwaits.tonkho.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.longwaits.tonkho.entity.BaseEntity;
import vn.longwaits.tonkho.entity.Item;
import vn.longwaits.tonkho.entity.Supplier;
import vn.longwaits.tonkho.entity.User;
import vn.longwaits.tonkho.repository.ItemRepository;
import vn.longwaits.tonkho.repository.SupplierRepository;
import vn.longwaits.tonkho.repository.UserRepository;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Tra cuu nhanh ten nguoi dung, nha cung cap, mat hang theo danh sach id. */
@Component
@RequiredArgsConstructor
public class Lookups {

    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;

    public Map<String, String> userNames(Collection<String> ids) {
        return userRepository.findAllById(clean(ids)).stream()
                .collect(Collectors.toMap(BaseEntity::getId, User::getFullName));
    }

    public Map<String, Supplier> suppliers(Collection<String> ids) {
        return supplierRepository.findAllById(clean(ids)).stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
    }

    public Map<String, Item> items(Collection<String> ids) {
        return itemRepository.findAllById(clean(ids)).stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
    }

    private static java.util.List<String> clean(Collection<String> ids) {
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}
