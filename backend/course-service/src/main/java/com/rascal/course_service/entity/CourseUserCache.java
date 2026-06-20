package com.rascal.course_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "course_user_cache")
public class CourseUserCache {

    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "gender")
    private Character gender;

    @Column(name = "batch")
    private Integer batch;

    @Column(name = "status")
    private String status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

}
