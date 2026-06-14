package com.rascal.user_service.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.rascal.user_service.dto.mapper.UserMapper;
import com.rascal.user_service.dto.request.UserPatchRequest;
import com.rascal.user_service.dto.request.UserRequest;
import com.rascal.user_service.dto.response.UserBulkImportResponse;
import com.rascal.user_service.dto.response.UserImportRowError;
import com.rascal.user_service.entity.Batch;
import com.rascal.user_service.entity.User;
import com.rascal.user_service.event.UserEventPublisher;
import com.rascal.user_service.repository.BatchRepository;
import com.rascal.user_service.repository.UserRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class UserService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final long MAX_IMPORT_FILE_SIZE = 8L * 1024L * 1024L;
    private static final int MAX_IMPORT_ROWS = 1_000;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final UserEventPublisher eventPublisher;

    public UserService(
        UserRepository userRepository,
        BatchRepository batchRepository,
        UserEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.eventPublisher = eventPublisher;
    }


    @Transactional(readOnly = true)
    public Page<User> getAllPaged(String name, Pageable pageable) {
        return getAllPaged(name, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getAllPaged(String name, Integer batchId, Pageable pageable) {
        String normalizedName = name == null ? "" : normalizeSearchName(name);

        return userRepository.searchActiveUsers(normalizedName, batchId, pageable);
    }

    public List<User> lookupByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        else if (ids.size() > 100) throw new BadRequestException("Too much");

        return userRepository.findByIdInAndDeletedAtIsNull(ids);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
    
    @Transactional(readOnly = true)
    public Boolean existByEmail(String email) {
        return userRepository.existsByEmailAndDeletedAtIsNull(normalizeEmail(email));
    }


    public User create(UserRequest request) {
        String email = normalizeEmail(request.email());
        if (existByEmail(email))
            throw new ConflictException("Email already exist");
        Batch batch = getActiveBatch(request.batch());
        String name = normalizeName(request.name());
        Character gender = normalizeGender(request.gender());

        User user = new User();
        UserMapper.toEntity(
            user, name, email, 
            gender, batch, STATUS_ACTIVE
        );
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        eventPublisher.userCreated(saved);

        return saved;
    }

    public User patch(Long id, UserPatchRequest request) {
        if (request.isEmptyPatch()) 
            throw new BadRequestException("Invalid patch");

        User user = getById(id);
        String oldEmail = user.getEmail();
        String oldName = user.getName();
        Character oldGender = user.getGender();
        Integer oldBatch = user.getBatch().getId();

        if (request.email() != null) {
            String email = normalizeEmail(request.email());
            if (userRepository.existsByEmailAndIdNotAndDeletedAtIsNull(email, id))
                throw new ConflictException("Email already exist");
            user.setEmail(normalizeEmail(email));
        }

        if (request.batch() != null) user.setBatch(getActiveBatch(request.batch()));
        if (request.name() != null) user.setName(normalizeName(request.name()));
        if (request.gender() != null) user.setGender(normalizeGender(request.gender()));
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        boolean profileChanged =
            !Objects.equals(oldName, saved.getName()) ||
            !Objects.equals(oldGender, saved.getGender()) ||
            !Objects.equals(oldBatch, saved.getBatch().getId());

        if (profileChanged) 
            eventPublisher.userProfileUpdated(saved);
        if (request.email() != null && !oldEmail.equals(saved.getEmail())) 
            eventPublisher.userEmailUpdated(saved, oldEmail);
        return saved;
    }


    public void deleteById(Long id) {
        User user = getById(id);
        user.setDeletedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        eventPublisher.userDeleted(saved);
    }


    public UserBulkImportResponse bulkImportExcel(MultipartFile file) {
        validateImportFile(file);

        List<UserImportRowError> errors = new ArrayList<>();
        Set<String> emailsInFile = new HashSet<>();
        int importedCount = 0;

        try (InputStream input = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) throw new BadRequestException("Excel sheet is empty");

            Map<String, Integer> columns = readHeader(sheet);
            int lastRow = sheet.getLastRowNum();
            if (lastRow > MAX_IMPORT_ROWS)
                throw new BadRequestException("Maximum import rows is " + MAX_IMPORT_ROWS);

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row)) continue;

                int excelRow = rowIndex + 1;
                try {
                    UserRequest request = toUserRequest(row, columns, formatter);
                    validateImportRequest(request);
                    String email = normalizeEmail(request.email());
                    if (!emailsInFile.add(email))
                        throw new ConflictException("Duplicate email in Excel file");

                    create(request);
                    importedCount++;
                } catch (RuntimeException err) {
                    errors.add(new UserImportRowError(excelRow, err.getMessage()));
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read Excel file");
        }

        return new UserBulkImportResponse(
            importedCount,
            errors.size(),
            errors
        );
    }



    private String normalizeName(String name) {
        String normalized = name.trim();
        if (normalized.isBlank())
            throw new BadRequestException("Name must be filled");

        return normalized;
    }

    private String normalizeSearchName(String name) {
        if (name == null || name.isBlank())
            return null;

        return name.trim();
    }

    private String normalizeEmail(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank())
            throw new BadRequestException("Email must be filled");

        return normalized;
    }

    private Character normalizeGender(Character gender) {
        char normalized = Character.toUpperCase(gender);
        if (normalized != 'L' && normalized != 'P')
            throw new BadRequestException("Gender must be L or P");

        return normalized;
    }

    private Batch getActiveBatch(Integer batchId) {
        return batchRepository.findByIdAndDeletedAtIsNull(batchId)
            .orElseThrow(() -> new NotFoundException("Batch not found"));
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("Excel file must be filled");
        if (file.getSize() > MAX_IMPORT_FILE_SIZE)
            throw new BadRequestException("Maximum Excel file size is 5MB");

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank())
            throw new BadRequestException("Excel filename must be filled");

        String normalized = filename.toLowerCase(Locale.ROOT);
        if (!normalized.endsWith(".xlsx") && !normalized.endsWith(".xls"))
            throw new BadRequestException("File type must be Excel .xlsx or .xls");
    }

    private Map<String, Integer> readHeader(Sheet sheet) {
        Row header = sheet.getRow(0);
        if (header == null) throw new BadRequestException("Excel header is missing");

        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            String name = normalizeHeader(formatter.formatCellValue(cell));
            if (!name.isBlank()) columns.put(name, cell.getColumnIndex());
        }

        requireColumn(columns, "name");
        requireColumn(columns, "email");
        requireColumn(columns, "batch");
        requireColumn(columns, "gender");
        return columns;
    }

    private void requireColumn(Map<String, Integer> columns, String name) {
        if (!columns.containsKey(name))
            throw new BadRequestException("Missing Excel column: " + name);
    }

    private UserRequest toUserRequest(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        return new UserRequest(
            cell(row, columns, formatter, "name"),
            cell(row, columns, formatter, "email"),
            parseInteger(cell(row, columns, formatter, "batch"), "batch"),
            parseGender(cell(row, columns, formatter, "gender"))
        );
    }

    private String cell(Row row, Map<String, Integer> columns, DataFormatter formatter, String column) {
        Integer index = columns.get(column);
        if (index == null) return "";

        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private Integer parseInteger(String raw, String field) {
        try { return Integer.valueOf(stripTrailingDecimal(raw)); }
        catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid " + field);
        }
    }

    private Character parseGender(String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("LAKI-LAKI") || normalized.equals("LAKI LAKI") || normalized.equals("MALE"))
            return 'L';
        if (normalized.equals("PEREMPUAN") || normalized.equals("FEMALE"))
            return 'P';
        if (normalized.length() == 1) return normalized.charAt(0);

        throw new BadRequestException("Gender must be L or P");
    }

    private void validateImportRequest(UserRequest request) {
        String name = normalizeName(request.name());
        if (name.length() < 3 || name.length() > 50)
            throw new BadRequestException("Name length is around 3 to 50 characters");

        String email = normalizeEmail(request.email());
        if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches())
            throw new BadRequestException("Invalid email");

        normalizeGender(request.gender());
    }

    private String stripTrailingDecimal(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        return normalized.endsWith(".0")
            ? normalized.substring(0, normalized.length() - 2)
            : normalized;
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

}
