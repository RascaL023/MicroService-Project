package com.rascal.user_service.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import com.rascal.user_service.dto.response.UserBulkImportResponse;
import com.rascal.user_service.dto.response.UserImportRowReport;
import com.rascal.user_service.entity.Batch;
import com.rascal.user_service.event.UserEventPublisher;
import com.rascal.user_service.repository.BatchRepository;
import com.rascal.user_service.repository.MajorRepository;
import com.rascal.user_service.repository.UserRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class UserBulkImportService {

    @Value("${app.excel.max-row:100}")
    private int maxImportRows;
    @Value("#{${app.excel.user-import.columns:{name:'name',email:'email',gender:'gender',major:'major'}}}")
    private Map<String, String> columnHeaders;
    @Value("${app.excel.user-import.header-start-row:0}")
    private int headerStartRow;
    @Value("${spring.servlet.multipart.max-file-size:8MB}")
    private DataSize maxImportFileSize;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final MajorRepository majorRepository;
    private final UserEventPublisher eventPublisher;

    public UserBulkImportService(
        JdbcTemplate jdbcTemplate,
        UserRepository userRepository,
        BatchRepository batchRepository,
        MajorRepository majorRepository,
        UserEventPublisher eventPublisher
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.majorRepository = majorRepository;
        this.eventPublisher = eventPublisher;
    }

    public UserBulkImportResponse importExcel(Integer batchId, MultipartFile file) {
        validateBatchId(batchId);
        validateImportFile(file);

        Batch batch = batchRepository.findByIdAndDeletedAtIsNull(batchId)
            .orElseThrow(() -> new NotFoundException("Batch not found"));

        List<UserImportRowReport> reports = new ArrayList<>();
        List<ImportRow> validRows = readRows(file, reports);
        Map<String, String> activeMajorIds = getActiveMajorIds(validRows);
        List<ImportRow> insertableRows = new ArrayList<>();
        for (ImportRow row : validRows) {
            if (!activeMajorIds.containsKey(row.majorId())) {
                reports.add(new UserImportRowReport(
                    row.excelRow(),
                    row.email(),
                    "FAILED",
                    "Major not found: " + row.majorId()
                ));
                continue;
            }
            insertableRows.add(row);
        }

        Map<String, Long> inserted = bulkInsert(batch, insertableRows);
        Set<Long> insertedIds = new HashSet<>(inserted.values());
        if (!insertedIds.isEmpty()) {
            userRepository.findByIdInAndDeletedAtIsNull(insertedIds)
                .forEach(eventPublisher::userCreated);
        }

        int createdCount = 0;
        int skippedCount = 0;
        for (ImportRow row : insertableRows) {
            if (inserted.containsKey(row.email())) {
                ++createdCount;
                reports.add(new UserImportRowReport(
                    row.excelRow(),
                    row.email(),
                    "CREATED",
                    "Created successfully"
                ));
            } else {
                ++skippedCount;
                reports.add(new UserImportRowReport(
                    row.excelRow(),
                    row.email(),
                    "SKIPPED",
                    "Email already registered"
                ));
            }
        }

        reports.sort((left, right) -> Integer.compare(left.row(), right.row()));
        int failedCount = (int) reports.stream()
            .filter(row -> row.status().equals("FAILED"))
            .count();

        return new UserBulkImportResponse(
            createdCount,
            skippedCount,
            failedCount,
            reports
        );
    }

    private List<ImportRow> readRows(MultipartFile file, List<UserImportRowReport> reports) {
        List<ImportRow> rows = new ArrayList<>();
        Set<String> emailsInFile = new HashSet<>();

        try (InputStream input = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) throw new BadRequestException("Excel sheet is empty");

            Map<String, Integer> columns = readHeader(sheet);
            int lastRow = sheet.getLastRowNum();

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            int importRowCount = 0;
            for (int rowIndex = headerStartRow + 1; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row)) continue;
                if (++importRowCount > maxImportRows)
                    throw new BadRequestException("Maximum import rows is " + maxImportRows);

                int excelRow = rowIndex + 1;
                try {
                    // Mapped by Config!!
                    String name = normalizeName(cell(row, columns, formatter, "name"));
                    String email = normalizeEmail(cell(row, columns, formatter, "email"));
                    Character gender = parseGender(cell(row, columns, formatter, "gender"));
                    String majorId = normalizeMajorId(cell(row, columns, formatter, "major"));

                    if (!emailsInFile.add(email))
                        throw new BadRequestException("Duplicate email in Excel file");

                    rows.add(new ImportRow(excelRow, name, email, gender, majorId));
                } catch (RuntimeException err) {
                    reports.add(new UserImportRowReport(
                        excelRow,
                        safeEmail(row, columns, formatter),
                        "FAILED",
                        err.getMessage()
                    ));
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read Excel file");
        }

        return rows;
    }

    private Map<String, Long> bulkInsert(Batch batch, List<ImportRow> rows) {
        if (rows.isEmpty()) return Map.of();

        StringBuilder sqlQuery = new StringBuilder("""
            INSERT INTO users (name, email, gender, status, batch_id, major_id, created_at)
            VALUES
        """);
        for (int index = 0; index < rows.size(); index++) {
            if (index > 0) sqlQuery.append(", ");
            sqlQuery.append("(?, ?, ?, ?, ?, ?, ?)");
        }
        sqlQuery.append("""
             ON CONFLICT (email) WHERE deleted_at IS NULL DO NOTHING
             RETURNING id, email
        """);
        LocalDateTime now = LocalDateTime.now();

        PreparedStatementCreator creator = (Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(sqlQuery.toString());
            int param = 0;
            for (ImportRow row : rows) {
                statement.setString(++param, row.name());
                statement.setString(++param, row.email());
                statement.setString(++param, String.valueOf(row.gender()));
                statement.setString(++param, STATUS_ACTIVE);
                statement.setInt(++param, batch.getId());
                statement.setString(++param, row.majorId());
                statement.setObject(++param, now);
            }

            return statement;
        };

        ResultSetExtractor<Map<String, Long>> extractor = (ResultSet result) -> {
            Map<String, Long> inserted = new HashMap<>();
            while (result.next()) 
                inserted.put(result.getString("email"), result.getLong("id"));
            
            return inserted;
        };

        return jdbcTemplate.query(creator, extractor);
    }

    private Map<String, String> getActiveMajorIds(List<ImportRow> rows) {
        if (rows.isEmpty()) return Map.of();

        Set<String> majorIds = new HashSet<>();
        rows.forEach(row -> majorIds.add(row.majorId()));
        return majorRepository.findByIdInAndDeletedAtIsNull(majorIds).stream()
            .collect(HashMap::new, (map, major) -> map.put(major.getId(), major.getId()), HashMap::putAll);
    }

    private void validateBatchId(Integer batchId) {
        if (batchId == null || batchId < 1)
            throw new BadRequestException("Batch must be filled");
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("Excel file must be filled");
        if (file.getSize() > maxImportFileSize.toBytes())
            throw new BadRequestException("Maximum Excel file size is " + maxImportFileSize.toMegabytes() + "MB");

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank())
            throw new BadRequestException("Excel filename must be filled");

        String normalized = filename.toLowerCase(Locale.ROOT);
        if (!normalized.endsWith(".xlsx") && !normalized.endsWith(".xls"))
            throw new BadRequestException("File type must be Excel .xlsx or .xls");
    }

    private Map<String, Integer> readHeader(Sheet sheet) {
        Row header = sheet.getRow(headerStartRow);
        if (header == null) throw new BadRequestException("Excel header is missing");

        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            String name = normalizeHeader(formatter.formatCellValue(cell));
            if (!name.isBlank()) columns.put(name, cell.getColumnIndex());
        }

        validateHeaders(columns);
        return columns;
    }

    private void validateHeaders(Map<String, Integer> columns) {
        if (columnHeaders == null || columnHeaders.isEmpty())
            throw new ConflictException("Excel column config is missing");

        for (String header : columnHeaders.keySet()) {
            String configuredHeader = configuredHeader(header);
            if (!columns.containsKey(configuredHeader))
                throw new BadRequestException("Missing Excel column: " + configuredHeader);
        }
    }

    private String cell(Row row, Map<String, Integer> columns, DataFormatter formatter, String field) {
        Integer index = columns.get(configuredHeader(field));
        if (index == null) return "";

        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private String safeEmail(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        try { return normalizeEmail(cell(row, columns, formatter, "email")); }
        catch (RuntimeException err) { return cell(row, columns, formatter, "email"); }
    }

    private String configuredHeader(String field) {
        String header = columnHeaders == null ? null : columnHeaders.get(field);
        if (header == null || header.isBlank())
            throw new BadRequestException("Missing Excel config for field: " + field);

        return normalizeHeader(header);
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank())
            throw new BadRequestException("Name must be filled");
        if (normalized.length() < 3 || normalized.length() > 50)
            throw new BadRequestException("Name length is around 3 to 50 characters");

        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank())
            throw new BadRequestException("Email must be filled");
        if (normalized.length() > 254 || !EMAIL_PATTERN.matcher(normalized).matches())
            throw new BadRequestException("Invalid email");

        return normalized;
    }

    private Character parseGender(String raw) {
        String normalized = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("LAKI-LAKI") || normalized.equals("LAKI LAKI") || normalized.equals("MALE"))
            return 'L';
        if (normalized.equals("PEREMPUAN") || normalized.equals("FEMALE"))
            return 'P';
        if (normalized.length() == 1) {
            char gender = normalized.charAt(0);
            if (gender == 'L' || gender == 'P') return gender;
        }

        throw new BadRequestException("Gender must be L or P");
    }

    private String normalizeMajorId(String majorId) {
        String normalized = majorId == null ? "" : majorId.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank())
            throw new BadRequestException("Major must be filled");

        return normalized;
    }

    private String normalizeHeader(String header) {
        return header == null
            ? ""
            : header.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }

    private boolean isBlankRow(Row row) {
        if (row == null) return true;

        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) return false;
        }

        return true;
    }

    private record ImportRow(
        int excelRow,
        String name,
        String email,
        Character gender,
        String majorId
    ) { }

}
