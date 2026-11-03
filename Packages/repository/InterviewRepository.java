package com.college.placement.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.college.placement.entity.Interview;
import com.college.placement.enums.InterviewResult;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    boolean existsByApplicationIdAndResult(Long applicationId, InterviewResult result);

    List<Interview> findByApplicationIdOrderByScheduledAtAsc(Long applicationId);

    @Query("select i from Interview i where (:applicationId is null or i.application.id = :applicationId) "
            + "and (:result is null or i.result = :result)")
    Page<Interview> filter(@Param("applicationId") Long applicationId, @Param("result") InterviewResult result,
            Pageable pageable);
}
