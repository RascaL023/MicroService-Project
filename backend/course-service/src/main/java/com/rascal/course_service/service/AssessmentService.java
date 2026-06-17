package com.rascal.course_service.service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.rascal.course_service.dto.mapper.AssessmentMapper;
import com.rascal.course_service.dto.request.AssessmentPatchRequest;
import com.rascal.course_service.dto.request.AssessmentRequest;
import com.rascal.course_service.dto.response.AssessmentResponse;
import com.rascal.course_service.entity.Assessment;
import com.rascal.course_service.entity.Group;
import com.rascal.course_service.entity.GroupMeeting;
import com.rascal.course_service.enumerated.AssessmentTypeEnum;
import com.rascal.course_service.enumerated.CourseStatusEnum;
import com.rascal.course_service.repository.AssessmentRepository;
import com.rascal.course_service.repository.GroupMeetingRepository;
import com.rascal.course_service.repository.GroupRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class AssessmentService {

    private static final String STORAGE_DIRECTORY = "assessments";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Map<String, String> MIME_TYPES = Map.of(
        "pdf", "application/pdf",
        "doc", "application/msword",
        "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final AssessmentRepository assessmentRepository;
    private final GroupRepository groupRepository;
    private final GroupMeetingRepository groupMeetingRepository;
    private final CoursePermissionService coursePermissionService;
    private final FileStorageService fileStorageService;
    private final FileStorageService.FileRule fileRule;

    public AssessmentService(
        AssessmentRepository assessmentRepository,
        GroupRepository groupRepository,
        GroupMeetingRepository groupMeetingRepository,
        CoursePermissionService coursePermissionService,
        FileStorageService fileStorageService
    ) {
        this.assessmentRepository = assessmentRepository;
        this.groupRepository = groupRepository;
        this.groupMeetingRepository = groupMeetingRepository;
        this.coursePermissionService = coursePermissionService;
        this.fileStorageService = fileStorageService;
        this.fileRule = new FileStorageService.FileRule(
            false,
            ALLOWED_EXTENSIONS,
            MIME_TYPES,
            "Assessment file type must be PDF or Microsoft Word",
            "Failed to store assessment file"
        );
    }

    @Transactional(readOnly = true)
    public Page<AssessmentResponse> getAllPaged(
        Long groupId,
        Long groupMeetingId,
        Long subjectId,
        String type,
        String title,
        Pageable pageable
    ) {
        return assessmentRepository
            .searchActiveAssessments(
                groupId,
                groupMeetingId,
                subjectId,
                normalizeSearchType(type),
                normalizeSearchTitle(title),
                pageable
            )
            .map(AssessmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Assessment getById(Long id) {
        return assessmentRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Assessment not found"));
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getByIdResponse(Long id) {
        return AssessmentMapper.toResponse(getById(id));
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> getByGroupId(Long groupId) {
        return assessmentRepository.findActiveByGroupIdOrderByDueAt(groupId)
            .stream().map(AssessmentMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Resource getFileResource(Long id) {
        Assessment assessment = getById(id);
        if (assessment.getStoredFilename() == null)
            throw new NotFoundException("Assessment file not found");

        return fileStorageService.load(physicalPath(assessment), "Assessment file not found");
    }

    public AssessmentResponse create(AssessmentRequest request, MultipartFile file) {
        Group group = getActiveGroup(request.groupId());
        coursePermissionService.requireGroupInstructor(group.getId());
        rejectInactiveGroup(group);

        AssessmentTypeEnum type = normalizeType(request.type());
        GroupMeeting meeting = resolveMeeting(request.groupMeetingId(), group.getId());
        validateMeetingRequirement(type, meeting);

        Assessment assessment = new Assessment();
        assessment.setGroup(group);
        assessment.setGroupMeeting(meeting);
        assessment.setType(type);
        assessment.setTitle(normalizeTitle(request.title()));
        assessment.setDescription(normalizeDescription(request.description()));
        assessment.setDueAt(request.dueAt());
        assessment.setCreatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty())
            applyStoredFile(assessment, fileStorageService.store(
                STORAGE_DIRECTORY,
                group.getId(),
                file,
                fileRule
            ));

        return AssessmentMapper.toResponse(assessmentRepository.save(assessment));
    }

    public AssessmentResponse updateById(Long id, AssessmentPatchRequest request, MultipartFile file) {
        if (request.isEmptyPatch() && (file == null || file.isEmpty()))
            throw new BadRequestException("Invalid patch");

        Assessment assessment = getById(id);
        Group oldGroup = assessment.getGroup();
        Group group = request.groupId() == null ? oldGroup : getActiveGroup(request.groupId());
        coursePermissionService.requireGroupInstructor(group.getId());
        rejectInactiveGroup(group);

        AssessmentTypeEnum type = request.type() == null ? assessment.getType() : normalizeType(request.type());
        boolean clearMeeting = request.type() != null
            && !requiresMeeting(type)
            && request.groupMeetingId() == null;
        GroupMeeting meeting = clearMeeting ? null : request.groupMeetingId() == null ?
            assessment.getGroupMeeting() : resolveMeeting(request.groupMeetingId(), group.getId());
        validateMeetingGroup(meeting, group.getId());
        validateMeetingRequirement(type, meeting);

        Path oldFile = assessment.getStoredFilename() == null ? null : physicalPath(assessment);
        assessment.setGroup(group);
        assessment.setGroupMeeting(meeting);
        assessment.setType(type);
        if (request.title() != null) assessment.setTitle(normalizeTitle(request.title()));
        if (request.description() != null) assessment.setDescription(normalizeDescription(request.description()));
        if (request.dueAt() != null) assessment.setDueAt(request.dueAt());

        if (file != null && !file.isEmpty()) {
            applyStoredFile(assessment, fileStorageService.store(
                STORAGE_DIRECTORY,
                group.getId(),
                file,
                fileRule
            ));
            fileStorageService.delete(oldFile);
        } else if (oldFile != null && !oldGroup.getId().equals(group.getId())) {
            fileStorageService.move(
                oldFile,
                STORAGE_DIRECTORY,
                group.getId(),
                assessment.getStoredFilename(),
                "Failed to move assessment file"
            );
            assessment.setFilePath(fileStorageService.publicPath(
                STORAGE_DIRECTORY,
                group.getId(),
                assessment.getStoredFilename()
            ));
        }

        assessment.setUpdatedAt(LocalDateTime.now());

        return AssessmentMapper.toResponse(assessmentRepository.save(assessment));
    }

    public void deleteById(Long id) {
        Assessment assessment = getById(id);
        coursePermissionService.requireGroupInstructor(assessment.getGroup().getId());
        assessment.setDeletedAt(LocalDateTime.now());

        assessmentRepository.save(assessment);
        fileStorageService.delete(assessment.getStoredFilename() == null ? null : physicalPath(assessment));
    }

    private Group getActiveGroup(Long groupId) {
        if (groupId == null) throw new BadRequestException("Group ID must be filled");

        return groupRepository.findByIdAndDeletedAtIsNull(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found"));
    }

    private GroupMeeting resolveMeeting(Long meetingId, Long groupId) {
        if (meetingId == null) return null;

        GroupMeeting meeting = groupMeetingRepository.findByIdAndDeletedAtIsNull(meetingId)
            .orElseThrow(() -> new NotFoundException("Group meeting not found"));
        if (!meeting.getGroup().getId().equals(groupId))
            throw new BadRequestException("Group meeting does not belong to selected group");

        return meeting;
    }

    private void validateMeetingRequirement(AssessmentTypeEnum type, GroupMeeting meeting) {
        if (requiresMeeting(type) && meeting == null)
            throw new BadRequestException("Assignment and quiz must be linked to a group meeting");

        if (type == AssessmentTypeEnum.QUIZ
            && meeting.getSubjectMaterial().getMeetingNumber() != null
            && meeting.getSubjectMaterial().getMeetingNumber() == 1)
            throw new BadRequestException("First meeting cannot have a quiz");
    }

    private boolean requiresMeeting(AssessmentTypeEnum type) {
        return type == AssessmentTypeEnum.ASSIGNMENT || type == AssessmentTypeEnum.QUIZ;
    }

    private void validateMeetingGroup(GroupMeeting meeting, Long groupId) {
        if (meeting != null && !meeting.getGroup().getId().equals(groupId))
            throw new BadRequestException("Group meeting does not belong to selected group");
    }

    private void rejectInactiveGroup(Group group) {
        if (group.getStatus() != CourseStatusEnum.ON_GOING)
            throw new BadRequestException("Group is not active");
    }

    private AssessmentTypeEnum normalizeSearchType(String type) {
        if (type == null || type.isBlank()) return null;

        return normalizeType(type);
    }

    private AssessmentTypeEnum normalizeType(String type) {
        if (type == null || type.isBlank())
            throw new BadRequestException("Assessment type must be filled");

        try { return AssessmentTypeEnum.from(type); }
        catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid assessment type");
        }
    }

    private String normalizeTitle(String title) {
        if (title == null) throw new BadRequestException("Assessment title must be filled");

        String normalized = title.trim();
        if (normalized.isBlank())
            throw new BadRequestException("Assessment title must be filled");

        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null) return null;

        String normalized = description.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeSearchTitle(String title) {
        return title == null || title.isBlank() ? "" : title.trim();
    }

    private void applyStoredFile(Assessment assessment, FileStorageService.StoredFile storedFile) {
        assessment.setOriginalFilename(storedFile.originalFilename());
        assessment.setStoredFilename(storedFile.storedFilename());
        assessment.setFilePath(storedFile.publicPath());
        assessment.setMimeType(storedFile.mimeType());
        assessment.setFileSize(storedFile.fileSize());
    }

    private Path physicalPath(Assessment assessment) {
        return fileStorageService.physicalPath(
            STORAGE_DIRECTORY,
            assessment.getGroup().getId(),
            assessment.getStoredFilename()
        );
    }
}
