package vn.longwaits.tonkho.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.longwaits.tonkho.entity.User;
import vn.longwaits.tonkho.service.UserService;
import vn.longwaits.tonkho.web.dto.Dtos.LoginRequest;
import vn.longwaits.tonkho.web.dto.Dtos.LoginResponse;
import vn.longwaits.tonkho.web.dto.Dtos.PasswordRequest;
import vn.longwaits.tonkho.web.dto.Dtos.ProfileRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return userService.login(req);
    }

    @GetMapping("/me")
    public User me() {
        return userService.me();
    }

    @PutMapping("/profile")
    public User profile(@Valid @RequestBody ProfileRequest req) {
        return userService.updateProfile(req);
    }

    @PutMapping("/password")
    public Map<String, String> password(@Valid @RequestBody PasswordRequest req) {
        userService.changePassword(req);
        return Map.of("message", "Đổi mật khẩu thành công");
    }
}
