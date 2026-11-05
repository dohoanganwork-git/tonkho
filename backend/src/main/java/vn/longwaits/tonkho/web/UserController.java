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
import org.springframework.web.bind.annotation.RestController;
import vn.longwaits.tonkho.entity.User;
import vn.longwaits.tonkho.service.UserService;
import vn.longwaits.tonkho.web.dto.Dtos.ResetPasswordRequest;
import vn.longwaits.tonkho.web.dto.Dtos.UserRequest;

import java.util.List;
import java.util.Map;

/** Quan ly tai khoan va phan quyen: chi Quan ly. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> list() {
        return userService.list();
    }

    @PostMapping
    public User create(@Valid @RequestBody UserRequest req) {
        return userService.create(req);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable String id, @Valid @RequestBody UserRequest req) {
        return userService.update(id, req);
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, String> reset(@PathVariable String id, @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(id, req.newPassword());
        return Map.of("message", "Đã đặt lại mật khẩu");
    }
}
