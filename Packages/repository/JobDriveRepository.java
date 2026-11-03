package com.college.placement.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.college.placement.entity.JobDrive;
import com.college.placement.enums.DriveStatus;

public interface JobDriveRepository extends JpaRepository<JobDrive, Long> {

    Page<JobDrive> findByStatus(DriveStatus status, Pageable pageable);

    boolean existsByCompanyId(Long companyId);

    @Query("select d from JobDrive d where d.status = :status "
            + "and (:keyword is null or lower(d.title) like lower(concat('%', :keyword, '%')) "
            + "or lower(d.company.name) like lower(concat('%', :keyword, '%'))) "
            + "and (:department is null or upper(d.eligibleDepartment) = 'ALL' "
            + "or lower(d.eligibleDepartment) = lower(:department))")
    Page<JobDrive> search(@Param("status") DriveStatus status, @Param("keyword") String keyword,
            @Param("department") String department, Pageable pageable);

    @Modifying
    @Query("update JobDrive d set d.status = :closed where d.status = :published and d.applicationDeadline < :today")
    int closeExpired(@Param("closed") DriveStatus closed, @Param("published") DriveStatus published,
            @Param("today") LocalDate today);
}
