package vn.longwaits.tonkho.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.common.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Luu file goc (hoa don, hop dong, minh chung, file POS, file kiem ke) tren o dia may chu.
 * Tren Railway can gan Volume vao thu muc UPLOAD_DIR de file khong mat khi deploy lai.
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp", "csv", "xlsx", "xls", "doc", "docx");

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /** Luu file vao thu muc con, tra ve duong dan tuong doi de ghi vao CSDL. */
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("File tải lên đang trống");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED.contains(ext)) {
            throw ApiException.badRequest("Định dạng file ." + ext + " không được hỗ trợ");
        }
        String safeName = slug(original.substring(0, original.length() - ext.length() - 1));
        String relative = folder + "/" + LocalDate.now().getYear() + "/"
                + UUID.randomUUID().toString().substring(0, 8) + "_" + safeName + "." + ext;
        Path target = resolve(relative);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Khong luu duoc file", e);
        }
        return relative;
    }

    public Resource load(String relative) {
        Path p = resolve(relative);
        if (!Files.exists(p)) {
            throw ApiException.notFound("Không tìm thấy file");
        }
        return new FileSystemResource(p);
    }

    private Path resolve(String relative) {
        Path p = root.resolve(relative).normalize();
        if (!p.startsWith(root)) {
            throw ApiException.badRequest("Đường dẫn file không hợp lệ");
        }
        return p;
    }

    public static String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase();
    }

    private static String slug(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        n = n.replaceAll("[^A-Za-z0-9-_]+", "-").replaceAll("-{2,}", "-");
        if (n.length() > 60) {
            n = n.substring(0, 60);
        }
        return n.isBlank() ? "file" : n;
    }
}
