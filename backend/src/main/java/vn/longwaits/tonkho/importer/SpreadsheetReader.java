package vn.longwaits.tonkho.importer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.common.ApiException;
import vn.longwaits.tonkho.storage.FileStorageService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Doc file CSV hoac Excel thanh danh sach dong. Ten cot duoc chuan hoa:
 * "Mã POS" -> "ma_pos", "Số lượng" -> "so_luong".
 */
public final class SpreadsheetReader {

    private SpreadsheetReader() {
    }

    public record ParsedSheet(List<String> headers, List<Map<String, String>> rows) {
        public boolean hasAny(String... cols) {
            for (String c : cols) {
                if (headers.contains(c)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static ParsedSheet read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("File tải lên đang trống");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = FileStorageService.extension(name);
        try (InputStream in = file.getInputStream()) {
            ParsedSheet sheet = switch (ext) {
                case "csv" -> readCsv(in);
                case "xlsx", "xls" -> readExcel(in);
                default -> throw ApiException.badRequest("Chỉ hỗ trợ file .csv, .xlsx hoặc .xls");
            };
            if (sheet.rows().isEmpty()) {
                throw ApiException.badRequest("File " + name + " không có dòng dữ liệu nào");
            }
            return sheet;
        } catch (IOException e) {
            throw ApiException.badRequest("Không đọc được file " + name + ", vui lòng kiểm tra định dạng");
        }
    }

    private static ParsedSheet readCsv(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        br.mark(4096);
        String first = br.readLine();
        if (first == null) {
            throw ApiException.badRequest("File CSV trống");
        }
        br.reset();
        char delimiter = count(first, ';') > count(first, ',') ? ';' : ',';
        Reader reader = skipBom(br);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        try (CSVParser parser = format.parse(reader)) {
            List<String> rawHeaders = parser.getHeaderNames();
            List<String> headers = rawHeaders.stream().map(SpreadsheetReader::normalize).toList();
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord rec : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                boolean blank = true;
                for (int i = 0; i < rawHeaders.size(); i++) {
                    String v = i < rec.size() ? rec.get(i).trim() : "";
                    if (!v.isEmpty()) {
                        blank = false;
                    }
                    row.put(headers.get(i), v);
                }
                if (!blank) {
                    rows.add(row);
                }
            }
            return new ParsedSheet(headers, rows);
        }
    }

    private static Reader skipBom(BufferedReader br) throws IOException {
        br.mark(1);
        int c = br.read();
        if (c != 0xFEFF) {
            br.reset();
        }
        return br;
    }

    private static ParsedSheet readExcel(InputStream in) throws IOException {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw ApiException.badRequest("File Excel không có dòng tiêu đề");
            }
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell c = headerRow.getCell(i);
                headers.add(normalize(c == null ? "" : fmt.formatCellValue(c)));
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> map = new LinkedHashMap<>();
                boolean blank = true;
                for (int i = 0; i < headers.size(); i++) {
                    Cell c = row.getCell(i);
                    String v = c == null ? "" : fmt.formatCellValue(c).trim();
                    if (!v.isEmpty()) {
                        blank = false;
                    }
                    map.put(headers.get(i), v);
                }
                if (!blank) {
                    rows.add(map);
                }
            }
            return new ParsedSheet(headers, rows);
        }
    }

    private static int count(String s, char ch) {
        int n = 0;
        for (char c : s.toCharArray()) {
            if (c == ch) {
                n++;
            }
        }
        return n;
    }

    /** Bo dau tieng Viet, chu thuong, thay ky tu dac biet bang "_". */
    public static String normalize(String header) {
        String n = Normalizer.normalize(header.replace("﻿", ""), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_");
        return n.replaceAll("^_+|_+$", "");
    }

    /** Lay gia tri cua cot dau tien co mat trong danh sach ten cot cho phep. */
    public static String pick(Map<String, String> row, String... keys) {
        for (String k : keys) {
            String v = row.get(k);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /** Doc so nguyen, chap nhan "1.000" hoac "1,000" hoac "12.0". */
    public static Integer parseInt(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String s = v.trim().replace(" ", "");
        if (s.matches("-?\\d+[.,]0{1,2}")) {
            s = s.substring(0, s.length() - s.replaceAll("^-?\\d+", "").length());
        }
        s = s.replace(".", "").replace(",", "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Doc so tien, chap nhan "90.000", "90,000", "90000". */
    public static java.math.BigDecimal parseMoney(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String s = v.trim().replace(" ", "").replace("đ", "").replace("₫", "").replace("VND", "");
        s = s.replace(".", "").replace(",", "");
        try {
            return new java.math.BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
