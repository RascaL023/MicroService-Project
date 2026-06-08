package com.rascal.course_service.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.rascal.course_service.dto.mapper.SubjectMapper;
import com.rascal.course_service.dto.request.SubjectPatchRequest;
import com.rascal.course_service.dto.request.SubjectRequest;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.repository.SubjectRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }


    public Subject getById(Long id) {
        return subjectRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Subject not found"));
    }

    public Page<Subject> getAll(Pageable pageable) {
        return subjectRepository.findByDeletedAtIsNull(pageable);
    }


    public Subject create(SubjectRequest request) {
        Subject subject = SubjectMapper.toEntity(request);
        subject.setCreatedAt(LocalDateTime.now());

        try { return subjectRepository.saveAndFlush(subject); } 
        catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Subject already exist");
        }
    }

    public Subject updateById(Long id, SubjectPatchRequest request) {
        if (request.isEmptyPatch()) 
            throw new BadRequestException("Invalid patch");

        Subject subject = getById(id);
        SubjectMapper.updateEntity(subject, request);

        try { return subjectRepository.saveAndFlush(subject); } 
        catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Subject already exist");
        }
    }


    public void deleteById(Long id) {
        Subject subject = getById(id);
        subject.setDeletedAt(LocalDateTime.now());

        subjectRepository.save(subject);
    }
    
}
