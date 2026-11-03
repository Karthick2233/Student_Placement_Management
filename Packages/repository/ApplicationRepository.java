package com.college.placement.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.college.placement.entity.Application;
import com.college.placement.enums.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByStudentIdAndJobDriveId(Long studentId, Long driveId);

    boolean existsByJobDriveId(Long driveId);

    Page<Application> findByStudentUserEmail(String email, Pageable pageable);

    List<Application> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("select a from Application a where (:driveId is null or a.jobDrive.id = :driveId) "
            + "and (:status is null or a.status = :status)")
    Page<Application> filter(@Param("driveId") Long driveId, @Param("status") ApplicationStatus status,
            Pageable pageable);

    @Query("select a.status, count(a) from Application a where a.jobDrive.id = :driveId group by a.status")
    List<Object[]> countByStatus(@Param("driveId") Long driveId);
}
