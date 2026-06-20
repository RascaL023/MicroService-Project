package com.rascal.course_service.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.course_service.dto.response.UserLookupResponse;
import com.rascal.course_service.entity.Assessment;
import com.rascal.course_service.entity.AssessmentGrade;
import com.rascal.course_service.entity.Enrollment;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.enumerated.AssessmentTypeEnum;
import com.rascal.course_service.enumerated.CourseRoleEnum;
import com.rascal.course_service.repository.AssessmentGradeRepository;
import com.rascal.course_service.repository.AssessmentRepository;
import com.rascal.course_service.repository.EnrollmentRepository;
import com.rascal.course_service.repository.SubjectRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
public class WeeklyGradeReportService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final AssessmentTypeEnum[] TYPES = AssessmentTypeEnum.values();
    private static final BigDecimal PASSING_SCORE = BigDecimal.valueOf(70);

    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentGradeRepository assessmentGradeRepository;
    private final SubjectRepository subjectRepository;
    private final CourseUserCacheService courseUserCacheService;

    public WeeklyGradeReportService(
        EnrollmentRepository enrollmentRepository,
        AssessmentRepository assessmentRepository,
        AssessmentGradeRepository assessmentGradeRepository,
        SubjectRepository subjectRepository,
        CourseUserCacheService courseUserCacheService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentGradeRepository = assessmentGradeRepository;
        this.subjectRepository = subjectRepository;
        this.courseUserCacheService = courseUserCacheService;
    }

    @Transactional(readOnly = true)
    public ReportFile generate(String academicYear, Long subjectId) {
        String normalizedAcademicYear = normalizeAcademicYear(academicYear);
        Subject subject = lookupSubject(subjectId);

        List<Enrollment> enrollments = enrollmentRepository.findActiveByAcademicYearSubjectAndRole(
            normalizedAcademicYear,
            subject.getId(),
            CourseRoleEnum.LEARNER
        );
        List<Assessment> assessments = assessmentRepository.findByAcademicYearAndSubject(
            normalizedAcademicYear,
            subject.getId()
        );

        Set<Long> userIds = enrollments.stream().map(Enrollment::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> assessmentIds = assessments.stream().map(Assessment::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, UserLookupResponse> usersById = courseUserCacheService.lookupByIds(userIds);
        List<Enrollment> reportEnrollments = enrollments.stream()
            .filter(enrollment -> !isDropOut(usersById.get(enrollment.getUserId())))
            .toList();
        Set<Long> reportUserIds = reportEnrollments.stream()
            .map(Enrollment::getUserId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<GradeKey, AssessmentGrade> gradesByKey = lookupGrades(assessmentIds, reportUserIds);

        ReportData data = buildReportData(normalizedAcademicYear, subject, reportEnrollments, assessments, usersById, gradesByKey);
        byte[] bytes = writeWorkbook(data);
        String filename = "weekly-grades-%s-%s.xlsx".formatted(
            safeFilename(normalizedAcademicYear),
            safeFilename(subject.getName())
        );

        return new ReportFile(filename, bytes);
    }

    private Subject lookupSubject(Long subjectId) {
        if (subjectId == null) throw new BadRequestException("subjectId is required");
        return subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found"));
    }

    private Map<GradeKey, AssessmentGrade> lookupGrades(Set<Long> assessmentIds, Set<Long> userIds) {
        if (assessmentIds.isEmpty() || userIds.isEmpty()) return Map.of();

        return assessmentGradeRepository.findByAssessmentIdInAndUserIdIn(assessmentIds, userIds)
            .stream()
            .collect(Collectors.toMap(
                grade -> new GradeKey(grade.getAssessment().getId(), grade.getUserId()),
                Function.identity(),
                (left, right) -> left
            ));
    }

    private ReportData buildReportData(
        String academicYear,
        Subject subject,
        List<Enrollment> enrollments,
        List<Assessment> assessments,
        Map<Long, UserLookupResponse> usersById,
        Map<GradeKey, AssessmentGrade> gradesByKey
    ) {
        Map<Long, LearnerRow> learners = new LinkedHashMap<>();
        Map<AssessmentTypeEnum, LinkedHashMap<String, AssessmentColumn>> columnMaps = emptyColumnMaps();

        for (Enrollment enrollment : enrollments) {
            learners.computeIfAbsent(
                enrollment.getUserId(),
                id -> new LearnerRow(id, usersById.get(id))
            );
        }

        List<Assessment> sortedAssessments = assessments.stream()
            .sorted(assessmentOrder())
            .toList();

        for (Assessment assessment : sortedAssessments) {
            LinkedHashMap<String, AssessmentColumn> columns = columnMaps.get(assessment.getType());
            AssessmentColumn column = columns.computeIfAbsent(
                assessmentColumnKey(assessment),
                key -> new AssessmentColumn(assessmentColumnLabel(assessment))
            );
            column.assessmentIds.add(assessment.getId());
        }

        for (LearnerRow row : learners.values()) {
            for (Assessment assessment : sortedAssessments) {
                AssessmentGrade grade = gradesByKey.get(new GradeKey(assessment.getId(), row.userId));
                if (grade != null) row.grades.put(assessment.getId(), grade.getScore());
            }
        }

        List<LearnerRow> sortedLearners = learners.values().stream()
            .sorted(Comparator
                .comparing((LearnerRow learner) -> learner.user == null || learner.user.name() == null ? "" : learner.user.name())
                .thenComparing(learner -> learner.userId))
            .toList();

        Map<AssessmentTypeEnum, List<AssessmentColumn>> columnsByType = new LinkedHashMap<>();
        for (AssessmentTypeEnum type : TYPES) {
            columnsByType.put(type, new ArrayList<>(columnMaps.get(type).values()));
        }

        return new ReportData(academicYear, subject, sortedLearners, columnsByType);
    }

    private Comparator<Assessment> assessmentOrder() {
        return Comparator
            .comparing(Assessment::getType)
            .thenComparing(
                this::assessmentMeetingNumber,
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing(
                Assessment::getDueAt,
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing(
                Assessment::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing(Assessment::getId);
    }

    private Integer assessmentMeetingNumber(Assessment assessment) {
        if (assessment == null || assessment.getGroupMeeting() == null) return null;
        if (assessment.getGroupMeeting().getSubjectMaterial() == null) return null;
        return assessment.getGroupMeeting().getSubjectMaterial().getMeetingNumber();
    }

    private Map<AssessmentTypeEnum, LinkedHashMap<String, AssessmentColumn>> emptyColumnMaps() {
        Map<AssessmentTypeEnum, LinkedHashMap<String, AssessmentColumn>> columns = new EnumMap<>(AssessmentTypeEnum.class);
        for (AssessmentTypeEnum type : TYPES) columns.put(type, new LinkedHashMap<>());
        return columns;
    }

    private byte[] writeWorkbook(ReportData data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            for (AssessmentTypeEnum type : TYPES) {
                writeAssessmentTypeSheet(workbook, styles, data, type, data.columnsByType.get(type));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException err) {
            throw new IllegalStateException("Failed to generate weekly grade report", err);
        }
    }

    private void writeAssessmentTypeSheet(
        Workbook workbook,
        Styles styles,
        ReportData data,
        AssessmentTypeEnum type,
        List<AssessmentColumn> columns
    ) {
        Sheet sheet = workbook.createSheet(uniqueSheetName(workbook, type.getDisplayName()));
        sheet.setDisplayGridlines(true);
        configureSheet(sheet, columns.size());
        sheet.showInPane(0, 0);
        sheet.setActiveCell(new CellAddress(0, 0));
        int rowIndex = 0;

        rowIndex = writeMeta(sheet, styles, rowIndex, data);
        rowIndex++;

        Row header = sheet.createRow(rowIndex++);
        header.setHeightInPoints(22);
        int col = 0;
        writeCell(header, col++, "No", styles.header);
        writeCell(header, col++, "Nama Peserta", styles.header);
        for (AssessmentColumn column : columns) {
            writeCell(header, col++, column.title, styles.header);
        }
        writeCell(header, col++, "Rata - rata", styles.header);
        writeCell(header, col, "Grade", styles.header);

        int no = 1;
        for (LearnerRow learner : data.learners) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(22);
            col = 0;
            writeNumber(row, col++, no++, styles.integer);
            writeCell(row, col++, learnerName(learner), styles.text);

            for (AssessmentColumn column : columns) {
                writeScore(row, col++, columnScore(learner, column), styles);
            }

            BigDecimal average = learnerAverage(learner, columns);
            writeScore(row, col++, average, styles);
            writeCell(row, col, grade(average), styles.grade);
        }

        sheet.createFreezePane(0, 6);
    }

    private int writeMeta(Sheet sheet, Styles styles, int rowIndex, ReportData data) {
        Row titleRow = sheet.createRow(rowIndex++);
        titleRow.setHeightInPoints(24);
        writeCell(titleRow, 0, "Laporan Nilai Mingguan", styles.title);

        Row academicYearRow = sheet.createRow(rowIndex++);
        academicYearRow.setHeightInPoints(20);
        writeCell(academicYearRow, 0, "Tahun Akademik", styles.label);
        writeCell(academicYearRow, 1, data.academicYear, styles.metaText);

        Row subjectRow = sheet.createRow(rowIndex++);
        subjectRow.setHeightInPoints(20);
        writeCell(subjectRow, 0, "Subject", styles.label);
        writeCell(subjectRow, 1, data.subject.getName(), styles.metaText);

        Row generatedRow = sheet.createRow(rowIndex++);
        generatedRow.setHeightInPoints(20);
        writeCell(generatedRow, 0, "Generated At", styles.label);
        writeCell(generatedRow, 1, LocalDateTime.now().format(DATE_TIME_FORMAT), styles.metaText);

        return rowIndex;
    }

    private void configureSheet(Sheet sheet, int assessmentColumnCount) {
        sheet.setDefaultRowHeightInPoints(20);
        sheet.setColumnWidth(0, 7 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        for (int i = 0; i < assessmentColumnCount; i++) {
            sheet.setColumnWidth(i + 2, 14 * 256);
        }
        sheet.setColumnWidth(assessmentColumnCount + 2, 14 * 256);
        sheet.setColumnWidth(assessmentColumnCount + 3, 10 * 256);
    }

    private void writeCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void writeNumber(Row row, int col, long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeScore(Row row, int col, BigDecimal value, Styles styles) {
        Cell cell = row.createCell(col);
        if (value != null) cell.setCellValue(value.doubleValue());
        cell.setCellStyle(isLowScore(value) ? styles.lowScore : styles.score);
    }

    private boolean isLowScore(BigDecimal value) {
        return value != null && value.compareTo(PASSING_SCORE) < 0;
    }

    private BigDecimal columnScore(LearnerRow learner, AssessmentColumn column) {
        List<BigDecimal> scores = column.assessmentIds.stream()
            .map(learner.grades::get)
            .filter(score -> score != null)
            .toList();
        return average(scores);
    }

    private BigDecimal learnerAverage(LearnerRow learner, List<AssessmentColumn> columns) {
        List<BigDecimal> scores = columns.stream()
            .map(column -> columnScore(learner, column))
            .filter(score -> score != null)
            .toList();
        return average(scores);
    }

    private BigDecimal average(List<BigDecimal> scores) {
        if (scores.isEmpty()) return null;
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private String grade(BigDecimal average) {
        if (average == null) return "";
        if (average.compareTo(BigDecimal.valueOf(85)) >= 0) return "A";
        if (average.compareTo(BigDecimal.valueOf(80)) >= 0) return "B";
        if (average.compareTo(BigDecimal.valueOf(70)) >= 0) return "C";
        if (average.compareTo(BigDecimal.valueOf(60)) >= 0) return "D";
        if (average.compareTo(BigDecimal.valueOf(40)) >= 0) return "E";
        return "F";
    }

    private String learnerName(LearnerRow learner) {
        if (learner.user != null && learner.user.name() != null && !learner.user.name().isBlank()) return learner.user.name();
        return "User #" + learner.userId;
    }

    private boolean isDropOut(UserLookupResponse user) {
        return user != null && "DROP_OUT".equalsIgnoreCase(user.status());
    }

    private String assessmentColumnKey(Assessment assessment) {
        return assessmentColumnLabel(assessment)
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }

    private String assessmentColumnLabel(Assessment assessment) {
        if (assessment == null || assessment.getType() == null) return "Nilai";
        if (assessment.getType() == AssessmentTypeEnum.MIDTERM || assessment.getType() == AssessmentTypeEnum.FINAL_EXAM)
            return "Nilai";

        Integer meetingNumber = assessmentMeetingNumber(assessment);
        if (meetingNumber != null) return assessment.getType().getDisplayName() + " " + meetingNumber;

        if (assessment.getTitle() != null && !assessment.getTitle().isBlank()) return assessment.getTitle().trim();
        return assessment.getType().getDisplayName();
    }

    private String uniqueSheetName(Workbook workbook, String rawName) {
        String base = sanitizeSheetName(rawName);
        String candidate = base;
        int suffix = 2;
        while (workbook.getSheet(candidate) != null) {
            String postfix = " " + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 31 - postfix.length())) + postfix;
        }
        return candidate;
    }

    private String sanitizeSheetName(String name) {
        String sanitized = name == null || name.isBlank() ? "Sheet" : name.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (sanitized.isBlank()) sanitized = "Sheet";
        return sanitized.substring(0, Math.min(sanitized.length(), 31));
    }

    private String safeFilename(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]+", "-");
    }

    private String normalizeAcademicYear(String academicYear) {
        if (academicYear == null || academicYear.isBlank()) throw new BadRequestException("academicYear is required");
        return academicYear.trim();
    }

    public record ReportFile(String filename, byte[] content) { }

    private record ReportData(
        String academicYear,
        Subject subject,
        List<LearnerRow> learners,
        Map<AssessmentTypeEnum, List<AssessmentColumn>> columnsByType
    ) { }

    private record GradeKey(Long assessmentId, Long userId) { }

    private static final class AssessmentColumn {
        private final String title;
        private final List<Long> assessmentIds = new ArrayList<>();

        private AssessmentColumn(String title) {
            this.title = title;
        }
    }

    private static final class LearnerRow {
        private final Long userId;
        private final UserLookupResponse user;
        private final Map<Long, BigDecimal> grades = new LinkedHashMap<>();

        private LearnerRow(Long userId, UserLookupResponse user) {
            this.userId = userId;
            this.user = user;
        }
    }

    private static final class Styles {
        private final CellStyle title;
        private final CellStyle label;
        private final CellStyle metaText;
        private final CellStyle header;
        private final CellStyle text;
        private final CellStyle integer;
        private final CellStyle score;
        private final CellStyle lowScore;
        private final CellStyle grade;

        private Styles(Workbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font lowScoreFont = workbook.createFont();
            lowScoreFont.setBold(true);
            lowScoreFont.setColor(IndexedColors.DARK_RED.getIndex());

            title = workbook.createCellStyle();
            title.setFont(titleFont);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            label = workbook.createCellStyle();
            label.setFont(boldFont);
            label.setVerticalAlignment(VerticalAlignment.CENTER);

            metaText = workbook.createCellStyle();
            metaText.setVerticalAlignment(VerticalAlignment.CENTER);

            header = workbook.createCellStyle();
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.BLACK.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            applyTableLayout(header);

            text = workbook.createCellStyle();
            text.setAlignment(HorizontalAlignment.LEFT);
            applyTableLayout(text);

            integer = workbook.createCellStyle();
            integer.setDataFormat(workbook.createDataFormat().getFormat("0"));
            integer.setAlignment(HorizontalAlignment.CENTER);
            applyTableLayout(integer);

            score = workbook.createCellStyle();
            score.setDataFormat(workbook.createDataFormat().getFormat("0.##"));
            score.setAlignment(HorizontalAlignment.CENTER);
            applyTableLayout(score);

            lowScore = workbook.createCellStyle();
            lowScore.cloneStyleFrom(score);
            lowScore.setFont(lowScoreFont);
            lowScore.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            lowScore.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            grade = workbook.createCellStyle();
            grade.setAlignment(HorizontalAlignment.CENTER);
            applyTableLayout(grade);
        }

        private void applyTableLayout(CellStyle style) {
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(true);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
        }
    }

}
