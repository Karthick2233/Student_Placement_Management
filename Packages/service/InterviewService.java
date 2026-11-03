package com.college.placement.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.placement.dto.ApplicationDto.InterviewRequest;
import com.college.placement.dto.ApplicationDto.InterviewResponse;
import com.college.placement.dto.ApplicationDto.InterviewResultRequest;
import com.college.placement.dto.ApplicationDto.InterviewUpdateRequest;
import com.college.placement.entity.Application;
import com.college.placement.entity.Interview;
import com.college.placement.enums.ApplicationStatus;
import com.college.placement.enums.InterviewResult;
import com.college.placement.exception.NotFoundException;
import com.college.placement.mapper.InterviewMapper;
import com.college.placement.repository.ApplicationRepository;
import com.college.placement.repository.InterviewRepository;

@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewRepository interviews;
    private final ApplicationRepository applications;
    private final InterviewMapper mapper;

    public InterviewService(InterviewRepository interviews, ApplicationRepository applications,
            InterviewMapper mapper) {
        this.interviews = interviews;
        this.applications = applications;
        this.mapper = mapper;
    }

    @Transactional
    public InterviewResponse schedule(InterviewRequest request) {
        Application application = findApplication(request.applicationId());
        if (application.getStatus() != ApplicationStatus.SHORTLISTED
                && application.getStatus() != ApplicationStatus.INTERVIEWING) {
            throw new IllegalStateException("Application must be SHORTLISTED or INTERVIEWING to schedule an interview");
        }
        Interview interview = interviews
                .save(new Interview(application, request.scheduledAt(), request.roundName().trim()));
        application.setStatus(ApplicationStatus.INTERVIEWING);
        log.info("Interview scheduled for application {}", application.getId());
        return mapper.toResponse(interview);
    }

    @Transactional(readOnly = true)
    public Page<InterviewResponse> list(Long applicationId, InterviewResult result, Pageable pageable) {
        return interviews.filter(applicationId, result, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InterviewResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> byApplication(Long applicationId) {
        findApplication(applicationId);
        return interviews.findByApplicationIdOrderByScheduledAtAsc(applicationId).stream()
                .map(mapper::toResponse).toList();
    }

    @Transactional
    public InterviewResponse update(Long id, InterviewUpdateRequest request) {
        Interview interview = find(id);
        requireScheduled(interview);
        interview.setScheduledAt(request.scheduledAt());
        interview.setRoundName(request.roundName().trim());
        log.info("Interview {} rescheduled", id);
        return mapper.toResponse(interview);
    }

    @Transactional
    public InterviewResponse updateResult(Long id, InterviewResultRequest request) {
        Interview interview = find(id);
        requireScheduled(interview);
        if (request.result() == InterviewResult.SCHEDULED) {
            throw new IllegalStateException("Result must be PASS, FAIL or NO_SHOW");
        }
        interview.updateResult(request.result(), request.feedback());
        if (request.result() == InterviewResult.FAIL) {
            interview.getApplication().setStatus(ApplicationStatus.REJECTED);
        }
        log.info("Interview {} result: {}", id, request.result());
        return mapper.toResponse(interview);
    }

    @Transactional
    public void delete(Long id) {
        Interview interview = find(id);
        requireScheduled(interview);
        interviews.delete(interview);
        log.info("Interview {} deleted", id);
    }

    private Interview find(Long id) {
        return interviews.findById(id).orElseThrow(() -> new NotFoundException("Interview not found"));
    }

    private Application findApplication(Long id) {
        return applications.findById(id).orElseThrow(() -> new NotFoundException("Application not found"));
    }

    private void requireScheduled(Interview interview) {
        if (interview.getResult() != InterviewResult.SCHEDULED) {
            throw new IllegalStateException("Interview result already recorded");
        }
    }
}
